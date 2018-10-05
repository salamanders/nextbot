


## Development

1. Login is through Firebase Auth, made easier with the Google Login button from firebaseui
2. Boolean flags are rendered in the config dialog, while numerical sliders are vertical -1.0..1.0 ranges
3. All inputs are "BIDI" (bi-directional inputs) with the state continually synced with firebase.


    cd ./website
    firebase serve 
    firebase deploy
    
## Resources and Links

### Project

* Project Console: https://console.firebase.google.com/project/nextbot0/overview
* Hosting URL: https://nextbot0.firebaseapp.com

### Reading

* https://github.com/material-components/material-components-web
    * https://github.com/material-components/material-components-web/blob/v0.40.0/docs/migrating-from-mdl.md
* https://stackoverflow.com/questions/15935837/how-to-display-a-range-input-slider-vertically
* https://css-tricks.com/styling-cross-browser-compatible-range-inputs-css/