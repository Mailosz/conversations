export class ConversationFeed extends HTMLElement {

    static observedAttributes = ["conversation-id", "session-key", "host"];

    constructor() {
        super();
    
    }

    connectedCallback() {

        const feedContainer = this.ownerDocument.createElement("div");
        feedContainer.classList.add("feed-container");
        this.appendChild(feedContainer);

        const feed = this.ownerDocument.createElement("div");
        feed.classList.add("message-feed");
        feedContainer.appendChild(feed);

        const messageWrite = this.makeEditable();
        this.appendChild(messageWrite);

        const waitRing = this.ownerDocument.createElement("div");
        waitRing.classList.add("wait-ring");
        waitRing.classList.add("hidden");
        waitRing.id = "wait-ring";
        this.appendChild(waitRing);

        const errors = this.ownerDocument.createElement("div");
        errors.id = "errors";
        this.appendChild(errors);

        this.prepare();
    }

    attributeChangedCallback(name, oldValue, newValue) {
        if (name == "conversation-id") {
            if (oldValue != newValue) {
                this.conversationId = newValue;
            }
        } else if (name == "user-key") {
            if (oldValue != newValue) {
                this.userKey = newValue;
            }
        } else if (name == "session-key") {
            if (oldValue != newValue) {
                this.sessionKey = newValue;
            }
        } else if (name == "host") {
            if (oldValue != newValue) {
                this.host = newValue;
            }
        }
    }

    getFeed() {
        return this.firstElementChild.firstElementChild;
    }

    prepare() {
        errors.innerHTML = "";

        if (!this.conversationId || !this.sessionKey || !this.host) {
            return;
        }

        const waitRing = document.getElementById("wait-ring");
        waitRing.classList.remove("hidden");

        this.getFeed().innerHTML = "";

        const request = new Request(this.host + "/conversation/" + this.conversationId, {
            method: "GET",
            headers: {"Session-key": this.sessionKey},
        });

        fetch(request).then((resp) => {
            if (resp.ok) {
                return resp.json();
            }else {
                throw resp.status + ": " + resp.statusText;
            }
        }).then((json) => {
            this.userKey = json.userKey;
            for (const message of json.messages) {
                this.addMessage(message.messageId, message.userKey, message.timestamp, message.data);
            }

            const subcsriptionCode = json.subscriptionCode;
            if (this.eventSource) {
                this.eventSource.close();
            }
            this.eventSource = new EventSource(this.host + "/subscribe/" + subcsriptionCode, {withCredentials: true});
            this.eventSource.onopen = function() {
                console.log("Connection to server opened.");
            };
            this.eventSource.onmessage = (event) => {
                console.log("update ", event.data);
    
                if (event.data) {
                    const message = JSON.parse(event.data);
    
                    this.addMessage(message.messageId, message.userKey, message.timestamp, message.data);
                }
    
            };
            this.eventSource.onerror = (error) => {
                console.log(error);
                this.showError("cannot fetch new messages");
            };

        }).catch((error) => {
            console.log(error);
            this.showError(error);
        }).finally(()=> {
            waitRing.classList.add("hidden");
        });


        
    }

    showError(error) {
        console.error(error);

        const errors = this.querySelector("#errors");

        const errorDiv = this.ownerDocument.createElement("div");
        if (error instanceof Event) {
            errorDiv.innerText = "Error occured (see console)";
        } else {
            errorDiv.innerText = "Error occured: " + error;
        }

        if (errors.childElementCount > 0) {
            errors.insertBefore(errorDiv, errors.lastElementChild);
        } else {
            errors.appendChild(errorDiv);

            const retry = document.createElement("button");
            retry.innerText = "Retry";
            retry.onclick = (event) => {
                this.prepare();
            };
            errors.appendChild(retry);
        }
    }

    addMessage(messageId, userKey, timestamp, messageData) {

        const feed = this.getFeed();

        let messageContainer = null;
        if (messageId) {
            messageContainer = feed.querySelector("#"+CSS.escape(messageId));
        }
        if (!messageContainer) {
            messageContainer = this.ownerDocument.createElement("div");
            messageContainer.classList.add("message");
            if (messageId) messageContainer.id = messageId;
            messageContainer.userKey = userKey;
            if (userKey == this.userKey) {
                messageContainer.classList.add("mine");
            }
            feed.appendChild(messageContainer);
        } else {
            messageContainer.textContent = "";
        }

        const messageContent = this.ownerDocument.createElement("div");
        messageContent.textContent = messageData;
        messageContainer.appendChild(messageContent);

        
        const messageInfo = this.ownerDocument.createElement("div");
        messageInfo.classList.add("info");
        if (timestamp) {
            const date = new Date(timestamp);
            messageInfo.textContent = date.toLocaleString();
        }
        messageContainer.appendChild(messageInfo);

        return messageContainer;

    }

    sendMessage(text) {
        if (text.trim().length == 0) {
            console.log("Message empty");
            return;
        }
            
        const dummy = this.addMessage(null, this.userKey, null, text);
        
        const request = new Request(this.host + "/conversation/" + this.conversationId, {
            method: "POST",
            headers: {
                "Content-type": "application/json",
                "Session-key": this.sessionKey
            },
            body: JSON.stringify({
                data: text,
            })
        });

        fetch(request).then((resp) => {
            if (resp.ok) {
                return resp.json();
            }else {
                console.error(resp)
            }
        }).then((json) => {
            dummy.remove();
            this.addMessage(json.messageId, json.userKey, json.timestamp, json.data);
        }).catch((error) => {
            const errorDiv = document.createElement("div");
            errorDiv.classList.add("error");
            errorDiv.innerText = "Nie udało się wysłać wiadomości";

            const retryButton = document.createElement("button");
            retryButton.innerText = "Retry";

            retryButton.onclick = (event) => {
                dummy.remove();
                this.sendMessage(text);
            };

            errorDiv.appendChild(retryButton);
            dummy.appendChild(errorDiv);
        }).finally(()=>{

        });

    }

    makeEditable(div) {

        const textarea = this.ownerDocument.createElement("textarea");
        textarea.style.flex = "1";
        
        if (div) {
            div.innerHTML = "";
            textarea.value = this.textContent;
        } else {
            div = this.ownerDocument.createElement("div");
        }
        div.appendChild(textarea);
        div.classList.add("editable");

        div.appendChild(this.ownerDocument.createElement("br"));
        
        const sendButton = this.ownerDocument.createElement("button");
        sendButton.textContent = "Send";
        div.appendChild(sendButton);

        sendButton.onclick = (event) => {
            const text = textarea.value;
            this.sendMessage(text);
            textarea.value = "";
            textarea.focus();
        }

        textarea.onkeydown = (event) => {
            if (event.key == "Enter" && !event.ctrlKey && !event.shiftKey) {
                const text = textarea.value;
                this.sendMessage(text);
                event.preventDefault();
                textarea.value = "";
                textarea.focus();
            }
        }

        return div;
    }

}

window.customElements.define("conversation-feed", ConversationFeed);