import os
import os.path
import locale
from time import gmtime, strftime


def write_json_to_file(json):
    time = gmtime()
    path = strftime("%Y/%m/%d/", time)
    file_name = strftime("%H%M%S", time) + '.json'
    file_with_path = __BASE_PATH + path + file_name
    __print_json(json, file_with_path)
    __write_script_for_html(json)


def __write_script_for_html(json):
    file = "script.js"
    locale.setlocale(locale.LC_ALL, "en_US.UTF-8")
    text = """
function convert() {{
   resetHtml()
   let jsonData = {json_data};

   let container = document.getElementById("container");
   let table = document.createElement("table");
   let thead = document.createElement("thead");
   let tr = document.createElement("tr");

   let thEntryTime = document.createElement("th");
   thEntryTime.innerText = "Entry Time";
   tr.appendChild(thEntryTime);
   let thText = document.createElement("th");
   thText.innerText = "Text";
   tr.appendChild(thText);

   thead.appendChild(tr);
   table.append(tr)

   jsonData.forEach((item) => {{
      let tr = document.createElement("tr");

      let tdEntryTime = document.createElement("td");
      tdEntryTime.innerText = item['entryTime'];
      tr.appendChild(tdEntryTime);

      let tdText = document.createElement("td");
      tdText.innerText = '- ' + item['text'];
      tr.appendChild(tdText);

      table.appendChild(tr);
   }});
   container.appendChild(table)
}}

function resetHtml() {{
    let container = document.getElementById("container");
    for (const child of container.children) {{
        child.remove();
    }}
}}
    """.format(json_data=json)

    with open(file, 'w') as file:
        file.writelines(text)


def __print_json(json, file_with_path):
    with __safe_open(file_with_path) as file:
        file.writelines(json)


def __safe_open(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    return open(path, 'w')


__BASE_PATH = "entries/"
