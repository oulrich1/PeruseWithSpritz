# Peruse with Spritz
### Made with simplicity in mind to efficiently read with Spritz.

## Functionality 
- Users can share e-pubs from the FS to the app.
- Users can share text to the app
- Users can share web pages to the app
- It provides a text fragment in which the user can paste or type text to spritz.
- Anything that is spritzed is saved to the phone's local database in the "recent" perusals list
- Perusals can be removed
- Perusals can be renamed
- Perusals' text can NOT be edited
- Spritz Text Partitions are temporary and changes made to them are not persisted

## Note about saving the Perusals:
The title of the perusals is limited to the first three words of the text.
If the Spritzing was done on a URL then the text is not saved, rather, only
the url is saved. In that case, the title is set to the part after the web protocol
"http://" stuff in the url. (Maybe the app should just save the text and not the
URL to prevent multiple spritz web scrapes?)

## Known Issues:
- Spritz text is too small on some devices. (Update epub text equivalently)
- Main activity: Choosing intent assumes "*/*" intent type is for e-pubs, should just be "application/epub+zip". On phone and simulator, sharing/sending epub from filesystem with the app created an intent with type "*/*". unexpected..
- E-Pub UI is plain
- Text from an e-pub "page" is saved to recent list when Spritzed. Subject to change.

## Todo
- Perusal Sharing 
- Figure out a way to easily access the book based on isbn number
- PDF support

- Wearable Spritz Views
- Font settings
- Instructions on how to use app
- Share URL that points to EPUB on internet (download epub and open as usual..)





