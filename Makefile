all: sources.txt
	javac -d build @sources.txt
	rm sources.txt

sources.txt: src
	find . -iname '*.java' > sources.txt

test: build
	cd build; java server.Server 5555 & \
	java client.Client localhost 5555 user1; \
	java client.Client localhost 5555 user2; \
	java client.Client localhost 5555 user3; \
	kill $$!

clean:
	rm -r build
	find . -iname '*javac*' -delete
