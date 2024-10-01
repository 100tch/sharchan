const langBtn = document.getElementById('lang-toggle');

langBtn.onclick = () => {
	const currentUrl = window.location.href.split('#')[0];

	const urlParams = new URLSearchParams(window.location.search);
	const langParam = urlParams.get('lang');

	if(langParam) {
		urlParams.delete('lang');
	} else {
		urlParams.append('lang', 'ru');
	}

	const newUrl = `${window.location.origin}${window.location.pathname}${urlParams.toString() ? '?' + urlParams.toString() : ''}${window.location.hash}`;
	window.location.href = newUrl;
}

document.addEventListener("DOMContentLoaded", () => {
	const langParam = new URLSearchParams(window.location.search).get('lang');
	if(langParam) {
		const convertableLinks = document.querySelectorAll('.takes-parameters');
		convertableLinks.forEach(link => {
			const url = new URL(link.href);
			url.searchParams.set('lang', langParam);
			link.href = url.toString();
		});
	}
});
