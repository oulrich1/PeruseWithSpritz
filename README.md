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
- OCR could be better ( Improve filtering or perform isbn scanning instead, to find a digital copy )

## Note about saving the Perusals:
The title of the perusals is limited to the first three words of the text.
If the Spritzing was done on a URL then the text is not saved, rather, only
the url is saved. In that case, the title is set to the part after the web protocol
"http://" stuff in the url. (Maybe the app should just save the text and not the
URL to prevent multiple spritz web scrapes?)

## Known Issues:
- URL Link sharing opens the spritz fragment (through the 'peruse controller' aka edittext fragment) and the url is saved in a recent perusal item, but Spritz fails to load and "spritz" the url.. The current user workaround is to go to the recent perusals list and select the url just shared/added. then it works flawlessly. 
- Main activity: Choosing intent assumes "*/*" intent type is for e-pubs, should just be "application/epub+zip". On phone and simulator, sharing/sending epub from filesystem with the app created an intent with type "*/*". unexpected..
- E-Pub UI is plain. ( Buttons or no buttons? Swipey views! )
- Text from an e-pub "page" is saved to recent list when Spritzed. Subject to change.
- Canceling during ocr causes app to crash..

## Todo
- Wearable Spritz Views. If wearable is present, send text data to the watch instead on "Spritz"
- Implement Spritz view subclass that ties in with the wearable 'fragments' ( to create views while computing the tings )
- Feature to change font type for reading purposes
- seperate text catagories: text, url, epub, images
- Share URL to epub
- Fix issues

## Long Term
- Figure out a way to easily access the book based on isbn number
- Perusal Sharing 
- PDF support?

