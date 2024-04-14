var lastId;

document.addEventListener("DOMContentLoaded", async function () {
    const responseForLastId = await fetch("/last_id");
    lastId = parseInt(await responseForLastId.text());
    const responseForServerData = await fetch("/server_data");
    var serverData = await responseForServerData.json();
    var trade_types = ["trade_preset", "trade_custom"]
    serverData.forEach(item => {
        if (trade_types.includes(item.type))
            addTradeCard(false, item);
    });
});

async function updateServer(card, action) {
    var id = card.querySelector("#card-id").value;
    var type = card.querySelector("#trade-type").value;
    var preset = card.querySelector("#preset-name").value;
    var content = card.querySelector("#content-text").value;
    var active = card.querySelector("#card-enabler-switch").checked;

    var tradeData = {
        "id": id,
        "type": type === "preset" ? "trade_preset" : "trade_custom",
        "active": active
    };
    if (type === "preset")
        tradeData["name"] = preset;
    else
        tradeData["content"] = content;
    tradeData = { [action]: [tradeData] };

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/data_receiver");
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(tradeData));
}

document.getElementById("add-card-button").addEventListener("click", function () {
    addTradeCard(true);
});

function addTradeCard(isNew, item) {
    // Add template to top of container
    var template = document.querySelector("#trade-template").content.cloneNode(true);
    var cardContainer = document.getElementById("card-container");
    cardContainer.insertBefore(template, cardContainer.firstChild);

    // Get template elements
    var thisCard = document.getElementById("trade-card");

    var id = thisCard.querySelector("#card-id");

    var warningDiv = thisCard.querySelector("#no-type-warning");

    var cardEnablerDiv = thisCard.querySelector("#card-enabler");
    var cardEnablerSwitch = cardEnablerDiv.querySelector("#card-enabler-switch");
    var cardEnablerLabel = cardEnablerDiv.querySelector("#card-enabler-label");

    var deleteCardButton = thisCard.querySelector("#delete-card-button");

    var confirmDeletionModal = document.getElementById("modal-confirm-deletion");
    var deleteButtonModal = document.getElementById("delete-button-modal");

    var tradeSelect = thisCard.querySelector("#trade-type");

    var tradePresetDiv = thisCard.querySelector("#preset");
    var tradePreset = tradePresetDiv.querySelector("#preset-name");

    var tradeContentDiv = thisCard.querySelector("#advanced-trade-content");
    var tradeContent = tradeContentDiv.querySelector("#content-text");

    if (isNew) {
        id.value = "trade-" + (lastId + 1);
        lastId++;
    }
    else {
        id.value = item.id;
        switch (item.type) {
            case "trade_preset":
                tradeSelect.value = "preset";
                tradePresetDiv.hidden = false;
                tradePreset.value = item.name;
                break;
            case "trade_custom":
                tradeSelect.value = "custom";
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
        var selectedTrade = tradeSelect.value;
        if (selectedTrade !== "none") {
            warningDiv.hidden = true;
            cardEnablerDiv.hidden = false;
            cardEnablerSwitch.checked = false;
            changeColorAndLabel(false);
            switch (selectedTrade) {
                case "preset":
                    showElements(tradePresetDiv);
                    hideElements(tradeContentDiv);
                    break;
                case "custom":
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
        cardEnablerLabel.innerText = isActive ? "Attivo" : "Non attivo";
    }

    function showElements(...divs) {
        for (var div of divs) {
            div.hidden = false;
        }
    }

    function hideElements(...divs) {
        for (var div of divs) {
            div.hidden = true;
        }
    }
}