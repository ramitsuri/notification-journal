function getTime(date, tzString) {
    return new Date((new Date(date)).toLocaleString("en-US", {timeZone: tzString})).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
}

function postPreviousEntry(file) {
    fetch('/previous', {
        method: 'POST',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ "file": file })
    })
       .then(response => response.json())
       .then(response => console.log("Done"))
}

function copyText(id) {
   let text = document.getElementById(id).innerText

   // https://stackoverflow.com/a/65996386

   const textArea = document.createElement("textarea");
   textArea.value = text;
   textArea.style.position = "absolute";
   textArea.style.left = "-999999px";

   document.body.prepend(textArea);
   textArea.select();

   try {
       document.execCommand('copy');
   } catch (error) {
       console.error(error);
   } finally {
       textArea.remove();
   }
}