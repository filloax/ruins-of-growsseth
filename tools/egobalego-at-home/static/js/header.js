document.getElementById("switch-theme-button").addEventListener("click", async function () {
    await fetch("/switch_theme");
    location.reload()
});