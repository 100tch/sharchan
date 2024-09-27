(function() {
	const savedTheme = localStorage.getItem('theme');
	if (savedTheme) {
		if(savedTheme == 'dark') document.documentElement.classList.add('dark-theme');
	} else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
		document.documentElement.classList.add('dark-theme');
	}
})();
