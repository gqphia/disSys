all: Proxy.class IServer.class Server.class LRU.class

%.class: %.java
	javac $<

clean:
	rm -f *.class

setPath:
	setenv CLASSPATH ../sample:../lib
	
proxy1:
	env proxyport15440=13391 java Proxy 127.0.0.1 23181 tmp 1000000 &

server1:
	env proxyport15440=13391 java Server 23181 . &

rmCache:
	rm -rf tmp/*

testCat:
	env proxyport15440=13391 env LD_PRELOAD=../lib/lib440lib.so ../tools/440cat fileroot/../hello.txt

testRead:
	env proxyport15440=13391 env LD_PRELOAD=../lib/lib440lib.so ../tools/440read fileroot/.././fileroot/hello.txt

testWrite:
	cat Makefile | env proxyport15440=13391 env LD_PRELOAD=../lib/lib440lib.so ../tools/440write hello.txt 2

testRm:
	env proxyport15440=13391 env LD_PRELOAD=../lib/lib440lib.so ../tools/440rm fileroot/../hello.txt 
	cat world.txt | env proxyport15440=13391 env LD_PRELOAD=../lib/lib440lib.so ../tools/440write hello.txt 2
	