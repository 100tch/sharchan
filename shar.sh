#!/bin/bash

targeturl="http://localhost:3000"
# consts
BOLD="\033[1m"
END="\033[0m"

usage() {
	echo -e "
	shar.sh
	$BOLD-u --upload   <file>$END                       upload local file
	$BOLD-s --shorten  /f/<hash>$END                    link shortener
	$BOLD-g --get      /f/<hash> [output file]$END      get file
	$BOLD-m --message$END                               create and upload small text file
	$BOLD<any stdout>$END                               upload any stdout via text file

	author: unknown
	"
}

error() {
	echo -e "error: $1 command requires an argument\nuse '-h' or '--help' for usage information" >&2
	exit 2
}

main() {
	case "$1" in
		"--upload")
			if [ -n "$2" ]; then
				curl -# -T $2 $targeturl | tee
			else error $1; fi
			;;
		"--shorten")
			if [ -n "$2" ]; then
				curl -F "url=$targeturl$2" $targeturl/shorten
			else error $1; fi
			;;
		"--get")
			if [ -n "$2" ]; then
				if [ -n "$3" ]; then
					curl -# $targeturl$2 -o $3
					if [ $? != 0 ]; then exit 3; fi
				else
					timestamp=$(date +"%Y%m%d_%H%M%S")
					autoname=out_$timestamp
					curl -# $targeturl$2 -o $autoname
					if [ $? != 0 ]; then exit 3; fi

					mime_type=$(file --mime-type -b $autoname)
					case $mime_type in
						"audio/flac") ext="flac" ;;
						"image/gif") ext="gif" ;;
						"image/jpeg") ext="jpg" ;;
						"image/png") ext="png" ;;
						"image/svg+xml") ext="svg" ;;
						"video/mp4") ext="mp4" ;;
						"video/webm") ext="webm" ;;
						"video/x-matroska") ext="mkv" ;;
						"audio/mpeg") ext="mp3" ;;
						"text/plain") ext="txt" ;;
						"text/x-diff") ext="diff" ;;
						"application/pdf") ext="pdf" ;;
						*) ext="bin" ;;
					esac

					newfilename="$autoname.$ext"
					mv $autoname $newfilename

					if [[ $ext == "txt" ]]; then
						# display and deleting a text file that was
						# downloaded without explicitly saving
						echo ""
						cat ./$newfilename
						rm ./$newfilename
					else
						echo -e "file $BOLD$newfilename$END was saved"
					fi
				fi
			else error $1; fi
			;;
		"--message")
			echo -n "message: "
			read message
			echo -e -n "\nuploading...\n"
			echo $message | curl -T - $targeturl
			;;

		# short args
		"-u")
			main "--upload" $2 ;;
		"-s")
			main "--shorten" $2 ;;
		"-g")
			main "--get" $2 $3 ;;
		"-m")
			main "--message" ;;
		* )
			echo -e "invalid argument: $1\nuse '-h' or '--help' for usage information"
			exit 1
			;;
		esac
		exit 0
}

fromstdin() {
	tmpfile="/tmp/shar_$(date '+%H-%M-%S')"
	while read line; do
		echo $line | tee -a $tmpfile
	done
	echo -e -n "\nuploading...\n"
	curl -s -T "$tmpfile" $targeturl
	rm $tmpfile
}

if [ -n "$1" ]; then
	if [ "$1" != "-h" ] && [ "$1" != "--help" ]; then
		main $1 $2 $3
	else
		usage
	fi
else
	echo -e "use '-h' or '--help' for usage information\nread from stdin...\n"
	fromstdin
fi
