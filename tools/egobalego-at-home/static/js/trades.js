let lastId;

document.addEventListener("DOMContentLoaded", async function () {
    const responseForLastId = await fetch("/last_id");
    lastId = parseInt(await responseForLastId.text());
    const responseForServerData = await fetch("/server_data");
    let serverData = await responseForServerData.json();
    let trade_types = ["tradePreset", "tradeCustom"]
    serverData.forEach(item => {
        if (trade_types.includes(item.type))
            addTradeCard(false, item);
    });
});

async function updateServer(card, action) {
    let id = card.querySelector("#card-id").value;
    let type = card.querySelector("#trade-type").value;
    let preset = card.querySelector("#preset-name").value;
    let content = card.querySelector("#content-text").value;
    let active = card.querySelector("#card-enabler-switch").checked;

    let tradeData = {
        "id": id,
        "type": type,
        "active": active
    };
    if (type === "tradePreset")
        tradeData["name"] = preset;
    else
        tradeData["content"] = content;
    tradeData = { [action]: [tradeData] };

    let xhr = new XMLHttpRequest();
    xhr.open("POST", "/data_receiver");
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(tradeData));
}

document.getElementById("add-card-button").addEventListener("click", function () {
    addTradeCard(true);
});

function addTradeCard(isNew, item) {
    // Add template to top of container
    let template = document.querySelector("#trade-template").content.cloneNode(true);
    let cardContainer = document.getElementById("card-container");
    cardContainer.insertBefore(template, cardContainer.firstChild);

    // Get template elements
    let thisCard = document.getElementById("trade-card");

    let id = thisCard.querySelector("#card-id");

    let warningDiv = thisCard.querySelector("#no-type-warning");

    let cardEnablerDiv = thisCard.querySelector("#card-enabler");
    let cardEnablerSwitch = cardEnablerDiv.querySelector("#card-enabler-switch");
    let cardEnablerLabel = cardEnablerDiv.querySelector("#card-enabler-label");

    let deleteCardButton = thisCard.querySelector("#delete-card-button");

    let confirmDeletionModal = document.getElementById("modal-confirm-deletion");
    let deleteButtonModal = document.getElementById("delete-button-modal");

    let tradeSelect = thisCard.querySelector("#trade-type");

    let tradePresetDiv = thisCard.querySelector("#preset");
    let tradePreset = tradePresetDiv.querySelector("#preset-name");

    let tradeContentDiv = thisCard.querySelector("#advanced-trade-content");
    let tradeContent = tradeContentDiv.querySelector("#content-text");

    if (isNew) {
        id.value = "trade-" + (lastId + 1);
        lastId++;
    }
    else {
        id.value = item.id;
        tradeSelect.value = item.type;
        switch (item.type) {
            case "tradePreset":
                tradePresetDiv.hidden = false;
                tradePreset.value = item.name;
                break;
            case "tradeCustom":
                tradeContentDiv.hidden = false;
                tradeContent.value = item.content;
                break;
        }
        warningDiv.hidden = true;
        cardEnablerDiv.hidden = false;
        cardEnablerSwitch.checked = item.active
        changeColorAndLabel(item.active);
    }

    deleteCardButton.addEventListener("click", function () {
        if (tradeSelect.value === "none") {
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

    tradeSelect.addEventListener("change", function () {
        let selectedTrade = tradeSelect.value;
        if (selectedTrade !== "none") {
            warningDiv.hidden = true;
            cardEnablerDiv.hidden = false;
            cardEnablerSwitch.checked = false;
            changeColorAndLabel(false);
            switch (selectedTrade) {
                case "tradePreset":
                    showElements(tradePresetDiv);
                    hideElements(tradeContentDiv);
                    break;
                case "tradeCustom":
                    showElements(tradeContentDiv);
                    hideElements(tradePresetDiv);
                    break;
            }
        } else {
            warningDiv.hidden = false;
            cardEnablerDiv.hidden = true;
            thisCard.classList.remove("bg-danger-subtle", "bg-success-subtle");
            thisCard.classList.add("bg-dark-subtle");
            hideElements(tradePresetDiv, tradeContentDiv);
        }
    });

    cardContainer.querySelectorAll('input, select, textarea').forEach(function (element) {
        element.addEventListener('change', function () {
            updateServer(thisCard, tradeSelect.value !== "none" ? "add" : "remove");
        });
    });

    function changeColorAndLabel(isActive) {
        if (isActive)
            thisCard.classList.remove("bg-dark-subtle", "bg-danger-subtle");
        else
            thisCard.classList.remove("bg-dark-subtle", "bg-success-subtle");
        thisCard.classList.add(isActive ? "bg-success-subtle" : "bg-danger-subtle");
        cardEnablerLabel.innerText = isActive ? activeText : notActiveText;
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