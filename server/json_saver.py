import os
import os.path
import locale
from time import gmtime, strftime


def write_json_to_file(json):
    time = gmtime()
    path = strftime("%Y/%m/%d/", time)
    file_name = strftime("%H%M%S", time) + ".json"
    file_with_path = __BASE_PATH + path + file_name
    __print_json(json, file_with_path)
    __write_script_for_html(json)


def __write_script_for_html(json):
    file = "script.js"
    locale.setlocale(locale.LC_ALL, "en_US.UTF-8")
    script_js_text_file = open("script.js.txt", "r")
    script_js_text = script_js_text_file.read().format(json_data=json)

    with open(file, "w") as file:
        file.writelines(script_js_text)


def __print_json(json, file_with_path):
    with __safe_open(file_with_path) as file:
        file.writelines(json)


def __safe_open(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    return open(path, "w")


__BASE_PATH = "entries/"
