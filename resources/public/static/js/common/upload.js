const form = document.getElementById('upload-form');

if(form != null) {
	const uploadBtn = document.querySelector('#upload-form button');
	const inputField = document.getElementById('file');

	uploadBtn.addEventListener('click', () => {
		inputField.value = '';
		inputField.click();
	});

	inputField.addEventListener('change', () => {
		if(inputField.files.length > 0) {
			form.dispatchEvent(new Event('submit', { cancelable: true }));
		}
	});

	form.addEventListener('submit', (event) => {
		event.preventDefault();

		const file = inputField.files[0];
		const responseBlock = document.getElementById('response-of-form');
		const statusPre = document.getElementById('upload-status');
		const responsePre = document.getElementById('upload-response');

		if (file) {
			const xhr = new XMLHttpRequest();
			const formData = new FormData();
			formData.append('file', file);

			xhr.open('POST', '/');

			xhr.upload.addEventListener('progress', function(e) {
				if (e.lengthComputable) {
					const percentComplete = (e.loaded / e.total) * 100;
					statusPre.innerHTML = `Upload progress: ${Math.round(percentComplete)}%`;
				}
			});

			xhr.onload = function() {
				responseBlock.classList.add('visible');
				statusPre.innerHTML = `Status: ${xhr.status}`;
				responsePre.innerHTML = `${xhr.response}`;
				disableForm();
			};

			xhr.onerror = function() {
				responseBlock.classList.add('visible');
				statusPre.innerHTML = 'An error occurred during the upload.';
				disableForm();
			};
			xhr.send(formData);
		}
	});

	function disableForm() {
		uploadBtn.disabled = true;
	}
}
