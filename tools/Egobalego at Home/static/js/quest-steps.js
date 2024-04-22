document.addEventListener("DOMContentLoaded", function () {
    let step1Card = document.getElementById("step-1");
    let step1Switch = document.getElementById("step-1-switch");
    let step1Label = document.getElementById("step-1-label");

    let step2Card = document.getElementById("step-2");
    let step2Warning = document.getElementById("step-2-warning");
    let step2Enable = document.getElementById("step-2-enable");
    let step2Switch = document.getElementById("step-2-switch");
    let step2Label = document.getElementById("step-2-label");

    let step3Card = document.getElementById("step-3");
    let step3Warning = document.getElementById("step-3-warning");
    let step3Enable = document.getElementById("step-3-enable");
    let step3Switch = document.getElementById("step-3-switch");
    let step3Label = document.getElementById("step-3-label");

    getServerData();

    step1Switch.addEventListener("click", function () {
        changeColor(step1Card, step1Label, this.checked);
        if (this.checked) {
            updateServer(1, "add");
            enableCard(step2Card, step2Warning, step2Enable);
        } else {
            updateServer(1, "remove");
            disableCard(step2Card, step2Warning, step2Enable, step2Switch);
            disableCard(step3Card, step3Warning, step3Enable, step3Switch);
        }
    });

    step2Switch.addEventListener("click", function () {
        changeColor(step2Card, step2Label, this.checked);
        if (this.checked) {
            updateServer(2, "add");
            enableCard(step3Card, step3Warning, step3Enable);
        } else {
            updateServer(2, "remove");
            disableCard(step3Card, step3Warning, step3Enable, step3Switch);
        }
    });

    step3Switch.addEventListener("click", function () {
        updateServer(3, this.checked ? "add" : "remove");
        changeColor(step3Card, step3Label, this.checked);
    });

    function changeColor(stepCard, stepLabel, active) {
        if (active) {
            stepLabel.innerText = "Attivo"
            stepCard.classList.remove("bg-danger-subtle");
            stepCard.classList.add("bg-success-subtle");
        } else {
            stepLabel.innerText = "Non attivo"
            stepCard.classList.remove("bg-success-subtle");
            stepCard.classList.add("bg-danger-subtle");
        }
    }

    function enableCard(stepCard, stepWarning, stepEnable) {
        stepCard.classList.remove("bg-dark-subtle", "bg-success-subtle");
        stepCard.classList.add("bg-danger-subtle");
        stepWarning.setAttribute("hidden", true);
        stepEnable.removeAttribute("hidden");
    }

    function disableCard(stepCard, stepWarning, stepEnable, stepSwitch) {
        stepCard.classList.remove("bg-danger-subtle", "bg-success-subtle");
        stepCard.classList.add("bg-dark-subtle");
        stepWarning.removeAttribute("hidden");
        stepEnable.setAttribute("hidden", true);
        stepSwitch.checked = false;
    }


    async function getServerData() {
        const responseForLastId = await fetch("/last_id");
        lastId = parseInt(await responseForLastId.text());
        const responseForServerData = await fetch("/server_data");
        let serverData = await responseForServerData.json();
        serverData.forEach(item => {
            if (item.type === "questStep")
                if (item.id === "quest-step-1") {
                    step1Switch.checked = true;
                    changeColor(step1Card, step1Label, true);
                    enableCard(step2Card, step2Warning, step2Enable);
                } else if (item.id === "quest-step-2") {
                    step2Switch.checked = true;
                    changeColor(step2Card, step2Label, true);
                    enableCard(step3Card, step3Warning, step3Enable);
                } else if (item.id === "quest-step-3") {
                    step3Switch.checked = true;
                    changeColor(step3Card, step3Label, true);
                }
        });
    }

    function updateServer(step, action) {
        const dataTemplate = {
            "id": "",
            "type": "questStep",
            "name": "",
            "active": true
        };
        const questSteps = {
            1: "researcher_end_quest_start",
            2: "researcher_end_quest_zombie",
            3: "researcher_end_quest_leave",
        }
        let questData = [];
        if (action === "add") {
            for (let i = 1; i <= step; i++) {
                let tempStep = { ...dataTemplate };
                tempStep["id"] = "quest-step-" + i;
                tempStep["name"] = questSteps[i];
                questData.push(tempStep);
            }
        }
        else if (action === "remove") {
            for (let i = 3; i >= step; i--) {
                let tempStep = { ...dataTemplate };
                tempStep["id"] = "quest-step-" + i;
                tempStep["name"] = questSteps[i];
                questData.push(tempStep);
            }
        }
        questData = {[action]: questData}
        let xhr = new XMLHttpRequest();
        xhr.open("POST", "/data_receiver");
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.send(JSON.stringify(questData));
    }
});