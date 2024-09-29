const logo = document.getElementById('main-logo');
const scrollBtn = document.getElementById('scroll-up');
const themeBtn = document.getElementById('theme-toggle');
const themeIcon = document.getElementById('theme-icon');
const langBtn = document.getElementById('lang-toggle');
const menuBtn = document.getElementById('menu-toggle');
const closeDialogBtn = document.getElementById('close-dialog');
const grphCanvas = document.getElementById('retention-grph');
const dialogBlock = document.getElementById('overlay');

const inversobleElements = document.querySelectorAll('.can-be-invers');
const formattableNumbers = document.querySelectorAll('.can-be-format');
const header = document.querySelector('header');

function formatNumber(num) {
	return num.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

function prepareTheme(isDark) {
	// theme icon and logo
	if(isDark) {
		themeIcon.classList.remove('moon');
		themeIcon.classList.add('sun');
		logo.src = '/static/img/logo/white-variant.png';
	} else {
		themeIcon.classList.remove('sun');
		themeIcon.classList.add('moon');
		logo.src = '/static/img/logo/black-variant.png';
	}
	// inversoble elements
	if(inversobleElements != null) {
		inversobleElements.forEach((element) => {
			element.classList.toggle('invers', !isDark);
		});
	}
	// graphic
	if(grphCanvas != null) {
		if(isDark) {
			grphCanvas.classList.add('invers');
		} else {
			grphCanvas.classList.remove('invers');
		}
	}
}

function debounce(func, wait = 250) {
	let timeout;
	return function executedFunction(...args) {
		clearTimeout(timeout);
		timeout = setTimeout(() => func.apply(this, args), wait);
	};
}


function isElementInViewport(el) {
	var rect = el.getBoundingClientRect();
	return (
		rect.top >= 0 &&
		rect.left >= 0 &&
		rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
		rect.right <= (window.innerWidth || document.documentElement.clientWidth)
	);
}

function loadChartJs(callback) {
	var script = document.createElement('script');
	script.src = 'https://cdn.jsdelivr.net/npm/chart.js';
	script.onload = callback;
	document.body.appendChild(script);
}

if(document.documentElement.classList.contains('dark-theme')) {
	prepareTheme(true);
} else {
	// check theme in local storage
	const savedTheme = localStorage.getItem('theme');
	if(savedTheme) {
		if(savedTheme == "dark")
			prepareTheme(true);
		else
			prepareTheme(false);
	}
}

// format numbers
formattableNumbers.forEach((element) => {
	var number = element.innerText;
	element.innerText = formatNumber(number);
});

// add listeners
themeBtn.onclick = () => {
	document.documentElement.classList.toggle('dark-theme');

	var containDark = document.documentElement.classList.contains('dark-theme');
	containDark
		? prepareTheme(true)
		: prepareTheme(false);
	localStorage.setItem('theme', containDark ? "dark" : "light");
}

scrollBtn.onclick = () => {
	window.scrollTo({
		top: 0,
		behavior: 'smooth'
	});
}

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

menuBtn.addEventListener('click', () => {
	dialogBlock.showModal();
	document.body.style.overflow = 'hidden';
});

closeDialogBtn.addEventListener('click', () => {
	dialogBlock.close();
	document.body.style.overflow = 'auto';
});

var grphLoaded = false;
window.addEventListener('scroll', debounce(() => {
	if(window.scrollY > 300 && !header.classList.contains('scrolled')) {
		header.classList.add('scrolled');
		scrollBtn.style.display = "block";
	}
	if(window.scrollY < 20) {
		header.classList.remove('scrolled');
		scrollBtn.style.display = "none";
	}

	var grphContainer = document.getElementById('grph-container');

	if(grphCanvas != null && !grphLoaded && isElementInViewport(grphContainer)) {
		loadChartJs(() => {
			// draw grph
			const initialData = JSON.parse(document.getElementById('initial-data').textContent);
			const minAge = initialData['min-age'];
			const maxAge = initialData['max-age'];
			const maxSize = initialData['max-size'];
			const dataPoints = [];

			for(let fileSize = 1; fileSize <= maxSize; fileSize += ((1024 * 1024) * 4)) {
				const retention = minAge + (-maxAge + minAge) * Math.pow((fileSize / maxSize - 1), 3);
				dataPoints.push({ fileSize, retention });
			}

			const labels = dataPoints.map(point => `${(point.fileSize / (1024 * 1024)).toFixed(2)} MB`);
			const retentionData = dataPoints.map(point => point.retention);

			const data = {
				labels: labels,
				datasets: [
					{
						label: 'Retention',
						data: retentionData,
						borderColor: 'rgba(0, 0, 0, 1)',
						backgroundColor: 'rgba(204, 0, 51, 0.5)',
						pointStyle: 'circle',
						pointRadius: 10,
						pointHoverRadius: 15
					}
				]
			};
			const config = {
				type: 'line',
				data: data,
				options: {
					responsive: true,
					scales: {
						y: {
							title: {
								display: true,
								text: "Days"
							}
						}
					}
				}
			};
			new Chart(grphCanvas, config);
			grphLoaded = true;
		});
	}
}, 200));

window.onload = () => {
	const urlParams = new URLSearchParams(window.location.search);
	const lang = urlParams.get('lang');
	if(lang) {
		document.documentElement.lang = lang;
	}
}
