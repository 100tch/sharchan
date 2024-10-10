.PHONY: all build nginx

all: build nginx

build:
	@mkdir -p app
	lein uberjar
	mv target/uberjar/*-standalone.jar app/

nginx:
	cp nginx/app.conf.template /etc/nginx/sites-available/app-nginx.conf
	sudo ln -sf /etc/nginx/sites-available/app-nginx.conf /etc/nginx/sites-enabled/app
	sudo systemctl restart nginx

clean:
	lein clean
