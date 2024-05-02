let lastId;

document.addEventListener("DOMContentLoaded", async function () {
    const responseForLastId = await fetch("/last_id");
    lastId = parseInt(await responseForLastId.text());
    const responseForServerData = await fetch("/server_data");
    let serverData = await responseForServerData.json();
    let command_types = ["command", "operation"]
    serverData.forEach(item => {
        if (command_types.includes(item.type))
            addCommandCard(false, item);
    });
});

async function updateServer(card, action) {
    let id = card.querySelector("#card-id").value;
    let type = card.querySelector("#command-type").value;
    let content = card.querySelector("#command-content").value;
    let x = card.querySelector("#x-coord").value;
    let y = card.querySelector("#y-coord").value;
    let z = card.querySelector("#z-coord").value;
    let active = card.querySelector("#card-enabler-switch").checked;

    let commandData = {
        "id": id,
        "type": type === "manual" ? "command" : "operation",
        "active": active
    };
    if (type === "manual")
        commandData["content"] = content;
    else {
        commandData["name"] = type;
        if (type !== "rmResearcher" || type !== "rmTentWithGift") {
            commandData["x"] = parseInt(x) || 0;
            commandData["y"] = parseInt(y) || 0;
            commandData["z"] = parseInt(z) || 0;
        }
    }
    commandData = { [action]: [commandData] };

    let xhr = new XMLHttpRequest();
    xhr.open("POST", "/data_receiver");
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(commandData));
}

document.getElementById("add-card-button").addEventListener("click", function () {
    addCommandCard(true);
});

function addCommandCard(isNew, item) {
    // Add template to top of container
    let template = document.querySelector("#command-template").content.cloneNode(true);
    let cardContainer = document.getElementById("card-container");
    cardContainer.insertBefore(template, cardContainer.firstChild);

    // Get template elements
    let thisCard = document.getElementById("command-card");

    let id = thisCard.querySelector("#card-id");

    let warningDiv = thisCard.querySelector("#no-type-warning");

    let cardEnablerDiv = thisCard.querySelector("#card-enabler");
    let cardEnablerSwitch = cardEnablerDiv.querySelector("#card-enabler-switch");
    let cardEnablerLabel = cardEnablerDiv.querySelector("#card-enabler-label");

    let deleteCardButton = thisCard.querySelector("#delete-card-button");

    let confirmDeletionModal = document.getElementById("modal-confirm-deletion");
    let deleteButtonModal = document.getElementById("delete-button-modal");

    let commandSelect = thisCard.querySelector("#command-type");

    let manualCommandDiv = thisCard.querySelector("#manual-command");
    let commandContent = manualCommandDiv.querySelector("#command-content");

    let coordinatesDiv = thisCard.querySelector("#coordinates");
    let x = coordinatesDiv.querySelector("#x-coord");
    let y = coordinatesDiv.querySelector("#y-coord");
    let z = coordinatesDiv.querySelector("#z-coord");

    if (isNew) {
        id.value = "command-" + (lastId + 1);
        lastId++;
    }
    else {
        id.value = item.id;
        switch (item.type) {
            case "command":
                commandSelect.value = "manual";
                manualCommandDiv.hidden = false;
                commandContent.value = item.content;
                break;
            case "operation":
                commandSelect.value = item.name;
                if (item.name !== "rmResearcher" || item.name !== "rmTentWithGift") {
                    coordinatesDiv.hidden = false;
                    x.value = item.x;
                    y.value = item.y;
                    z.value = item.z;
                }
                break;
        }
        warningDiv.hidden = true;
        cardEnablerDiv.hidden = false;
        cardEnablerSwitch.checked = item.active
        changeColorAndLabel(item.active);
    }

    deleteCardButton.addEventListener("click", function () {
        if (commandSelect.value === "none") {
            thisCard.remove();
        } else {
            const modal = new bootstrap.Modal(confirmDeletionModal);
            modal.show();
            deleteButtonModal.addEventListener("click", function () {
                updateServer(thisCard, "remove");
                thisCard.remove();
            });
        }
    });

    cardEnablerSwitch.addEventListener("click", function () {
        changeColorAndLabel(this.checked);
    });

    commandSelect.addEventListener("change", function () {
        let selectedCommand = commandSelect.value;
        if (selectedCommand !== "none") {
            warningDiv.hidden = true;
            cardEnablerDiv.hidden = false;
            cardEnablerSwitch.checked = false;
            changeColorAndLabel(false);
            switch (selectedCommand) {
                case "manual":
                    showElements(manualCommandDiv);
                    hideElements(coordinatesDiv);
                    break;
                case "rmResearcher":
                case "rmTentWithGift":
                    hideElements(manualCommandDiv, coordinatesDiv);
                    break;
                case "tpResearcher":
                case "spawnResearcher":
                case "rmTent":
                    showElements(coordinatesDiv);
                    hideElements(manualCommandDiv);
                    break;
            }
        } else {
            warningDiv.hidden = false;
            cardEnablerDiv.hidden = true;
            thisCard.classList.remove("bg-danger-subtle", "bg-success-subtle");
            thisCard.classList.add("bg-dark-subtle");
            hideElements(manualCommandDiv, coordinatesDiv);
        }
    });

    cardContainer.querySelectorAll('input, select, textarea').forEach(function (element) {
        element.addEventListener('change', function () {
            updateServer(thisCard, commandSelect.value !== "none" ? "add" : "remove");
        });
    });

    function changeColorAndLabel(isActive) {
        if (isActive)
            thisCard.classList.remove("bg-dark-subtle", "bg-danger-subtle");
        else
            thisCard.classList.remove("bg-dark-subtle", "bg-success-subtle");
        thisCard.classList.add(isActive ? "bg-success-subtle" : "bg-danger-subtle");
        cardEnablerLabel.innerText = isActive ? "Attivo" : "Non attivo";
    }

    function showElements(...divs) {
        for (let div of divs) {
            div.hidden = false;
        }
    }

    function hideElements(...divs) {
        for (let div of divs) {
            div.hidden = true;
        }
    }
}