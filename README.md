# Peruse with Spritz
### Made with simplicity in mind to efficiently read with Spritz.

## Functionality 
- Users can share text or URL data via another app through share intents.
- Users can therefore open up a web page, highlight some text and share that text with the app.
- It provides a text fragment in which the user can paste or type text to spritz.
- Anything that is spritzed is saved to the phone's local databsse in the recent perusals list
- Perusals can be removed
- Perusals can be renamed
- Perusals' text can NOT be edited
- OCR could be better

## Note about saving the Perusals:
The title of the perusals is limited to the first three words of the text.
If the Spritzing was done on a URL then the text is not saved, rather, only
the url is saved. In that case, the title is set to the part after the web protocol
"http://" stuff in the url. (Maybe the app should just save the text and not the
URL to prevent multiple spritz web scrapes?)

## Known Issues:
- URL Link sharing opens the spritz fragment (through the 'peruse controller' aka edittext fragment) and the url is saved in a recent perusal item, but Spritz fails to load and "spritz" the url.. The current user workaround is to go to the recent perusals list and select the url just shared/added. then it works flawlessly. 

## Todo
- E-Pub Support
- PDF Support
	- share url to pdf/epub
	- share pdf/epub
- Wearable Spritz Views. If wearable is present, send text data to the watch instead on "Spritz"
- Fix issues

## Unicorns 
- share url to image
- Filtering for OCR
- Perusal Sharing 

