(ns jsqlon.io
  (:import [java.io BufferedReader File IOException PrintWriter]
           [java.nio.channels Channels SelectionKey]
           [jnr.enxio.channels NativeSelectorProvider]
           [jnr.unixsocket UnixServerSocket UnixServerSocketChannel UnixSocketAddress UnixSocketChannel]))

(defn- run [behaviour reader writer]
  (doseq [request (line-seq reader)]
    (.println writer (behaviour request))))

(defn- with-stdin [behaviour]
  (let [reader (java.io.BufferedReader. *in*)
        writer *out*]
    (run behaviour reader writer)))

(defn- client-actor [channel behaviour]
  (fn []
    (let [reader (BufferedReader. (Channels/newReader channel "UTF-8"))
          writer (PrintWriter. (Channels/newWriter channel "UTF-8") true)]
      (run behaviour reader writer)
      (.close channel))))

(defn- server-actor [channel selector behaviour]
  (fn []
    (let [client (.accept channel)]
      (.configureBlocking client false)
      (.register client
                 selector
                 (bit-or SelectionKey/OP_READ SelectionKey/OP_WRITE)
                 (client-actor client behaviour)))))

(defn- with-socket [socket-path behaviour]
  (let [socket-file (doto (File. socket-path) (.deleteOnExit))
        address (UnixSocketAddress. socket-file)
        channel (UnixServerSocketChannel/open)
        selector (.openSelector (NativeSelectorProvider/getInstance))]
    (do
      (.configureBlocking channel false)
      (.bind (.socket channel) address)
      (.register channel selector SelectionKey/OP_ACCEPT
                 (server-actor channel selector behaviour))

      (while (>= (.select selector) 0)
        (let [iterator (.iterator (.selectedKeys selector))]
          (doall (for [selected-key (iterator-seq iterator)]
                   (do
                     (.remove iterator)
                     ((.attachment selected-key))))))))))

(defn with-io [mode & args]
  (case mode
    :stdin with-stdin
    :socket #(with-socket (first args) %)))
