## sharchan

sharchan is a simple and insecure file hosting service created as part of an educational project. it is mainly intended for transferring small files using HTTP requests.

## installation 

make sure you have installed the:
	* make (for building)
	* nginx (optional (because you can use apache or otherwise) but recommended)
	* systemd (system is expected to have it)

## usage

after clone repo you can use

	$ make build

for build app. and 

	$ make nginx

for move config template to /etc/nginx and "activate" him

after all you can just change directory to app/

	$ cd app

and launch

	$ java -jar *-standalone.jar

note: before launching you will also need to move `config.edn` to the app directory and edit it for yourself

## client

repo contain `shar.sh` file. you can put him to the /bin/ directory and use simple interface for file sharing. for help type:

	$ shar.sh --help
