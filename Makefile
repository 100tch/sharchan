.PHONY: all build nginx

all: build nginx

build:
	@mkdir -p app
	lein uberjar
	mv target/uberjar/*-standalone.jar app/

nginx:
	cp -r nginx/app-nginx.conf.template /etc/nginx/sites-avilable/app-nginx.conf
	sudo ln -s /etc/nginx/site-avilable/app-nginx.conf /etc/nginx/site-enabled/
	sudo systemctl restart nginx

clean:
	lein clean
