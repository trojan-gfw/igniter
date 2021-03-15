# coding=utf-8
import os
import requests
from lxml import etree


def figure_out_download_url():
    html = requests.get('https://github.com/trojan-gfw/igniter-go-libs/releases/')
    if not html.ok:
        return None
    root = etree.HTML(html.content)
    first_release_entry_div = root.find('.//div[@class="release-entry"]')
    if first_release_entry_div is None:
        return None
    download_a = first_release_entry_div.find('.//div/div[2]/details/div/div/div[1]/a')
    url_sub_path = download_a.get('href')
    return 'https://github.com' + url_sub_path


def replace_golibs():
    golibs_path = './app/src/libs/golibs.aar'
    if os.path.exists(golibs_path):
        os.remove(golibs_path)
    os.rename('tmp.aar', golibs_path)


def download_golibs():
    print('Figuring out url to download golibs.aar ...')
    download_url = figure_out_download_url()
    if download_url is None:
        print('Failed to figure out download url')
        return
    print('Download url: {0} downloading ...'.format(download_url))
    r = requests.get(url=download_url, allow_redirects=True)
    if not r.ok:
        print('Download golibs.aar failed')
        return
    with open('tmp.aar', 'wb') as output:
        output.write(r.content)
    print('Download success')
    replace_golibs()
    print('Done')


def main():
    download_golibs()


if __name__ == '__main__':
    main()
