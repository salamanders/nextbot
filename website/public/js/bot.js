/*globals firebase, firebaseui */
/*jshint esversion: 6, unused:true  */

// globals
// users/$uId/devices/$deviceId/state
let uId = null,
    deviceId = null,
    dbRef = null;

const docFullyReady = () => new Promise(resolve => {
    if (document.readyState === 'complete') {
        resolve();
        return;
    }
    const onReady = () => {
        resolve();
        document.removeEventListener('DOMContentLoaded', onReady, true);
        window.removeEventListener('load', onReady, true);
    };
    document.addEventListener('DOMContentLoaded', onReady, true);
    window.addEventListener('load', onReady, true);
});

// https://github.com/firebase/firebaseui-web
const easyAuth = () => new Promise(resolve => {
    const user = firebase.auth().currentUser;
    if (user) {
        console.info('Authorized (existing):', user.displayName);
        resolve(user);
        return;
    }

    firebase.auth().onAuthStateChanged(user => {
        console.info('onAuthStateChanged');
        if (user) {
            console.info('Authorized (onAuthStateChanged):', user.displayName);
            resolve(user);
            return;
        }
        const uiConfig = {
            signInSuccessUrl: '/',
            signInOptions: [
                {
                    provider: firebase.auth.GoogleAuthProvider.PROVIDER_ID,
                    authMethod: 'https://accounts.google.com',
                    clientId: '433645037958-ebm21iq1226c5sgu51c73nffapcbibhj.apps.googleusercontent.com'
                }
            ]
            //credentialHelper: firebaseui.auth.CredentialHelper.GOOGLE_YOLO // Whitelist only :(
        };
        const ui = new firebaseui.auth.AuthUI(firebase.auth());
        const authDiv = document.createElement('div');
        authDiv.id = 'firebaseui-auth-container';
        document.querySelector('.wrapper').appendChild(authDiv);
        ui.start('#firebaseui-auth-container', uiConfig);
        console.info('Auth: no way to resolve from here, which is ok because likely going to get redirected.');
    });
});

// from https://stackoverflow.com/questions/18544890/onchange-event-on-input-type-range-is-not-triggering-in-firefox-while-dragging/37623959#37623959
const onRangeChange = (r, f) => {
    let n, c, m;
    r.addEventListener('input', function (e) {
        n = 1;
        c = e.target.value;
        if (c !== m) f(e);
        m = c;
    });
    r.addEventListener('change', function (e) {
        if (!n) f(e);
    });
};

const mostRecentDeviceId = () => new Promise(resolve => {
    console.info(`Waiting for most recent device in '/users/${uId}/devices'`);
    firebase.database().ref(`/users/${uId}/devices`).limitToFirst(1).on('child_added', snapshot => {
        const deviceId = snapshot.key;
        console.info(`Most recent device ID: ${deviceId}`);
        resolve(deviceId);
    });
});

const addBidi = (id, sampleValue) => {
    const type = typeof sampleValue;
    console.info('addBidi', id, sampleValue, type);
    const template = document.querySelector(`#bidi-template-${type}`);
    if(!['number', 'boolean'].includes(type)) {
        return;
    }
    Object.assign(template.content.querySelector('input'), {
        'name': id,
        'id': id
    });
    Object.assign(template.content.querySelector('label'), {
        'for': id,
        'textContent': id
    });

    document.querySelector(`#bidi-content-${type}`).appendChild(document.importNode(template.content, true));

    switch (type) {
        case 'number':
            onRangeChange(document.querySelector('#' + id), event => {
                const updates = {};
                updates[id] = Number(event.target.value);
                dbRef.update(updates);
            });
            break;
        case 'boolean':
            document.querySelector('#' + id).addEventListener('change', event => {
                const updates = {};
                updates[id] = event.target.checked;
                dbRef.update(updates);
            });
            break;
        default:
            console.warn(`Unknown bidi type: ${type}`);
    }

    // Listen for updates from the bot or other apps
    dbRef.child(id).on('value', snapshot=>{
        const newValue = snapshot.val();
        const inputElt = document.querySelector('#' + id);
        if (!inputElt) {
            console.warn(`Skipping element value update, did not find input: ${id}`);
            return;
        }
        console.debug('Got new value from network, updating element', id, newValue);
        switch (typeof newValue) {
            case 'boolean':
                inputElt.checked = newValue;
                break;
            case 'number':
                inputElt.value = newValue;
                break;
            default:
                console.warn(`Unable to update state of ${id} because '${newValue}' is unknown type '${typeof newValue}'`);
        }
    });
};

// Flag configuration overlay dialog open and close
const instrumentConfigDialog = () => {
    const flagsDialog = document.querySelector('#flags-dialog');

    flagsDialog.addEventListener('click', event => {
        console.debug(`Clicked related to the dialogOfFlags`);
        const rect = flagsDialog.getBoundingClientRect();
        const isInDialog = (rect.top <= event.clientY &&
            event.clientY <= rect.top + rect.height &&
            rect.left <= event.clientX &&
            event.clientX <= rect.left + rect.width);
        if (!isInDialog) {
            if (flagsDialog.open) {
                flagsDialog.close();
            }
        }
    });
    document.querySelector('#flags-dialog-close').addEventListener('click', () => {
        if (flagsDialog.open) {
            flagsDialog.close();
        }
    });
    document.querySelector('#flags-button').addEventListener('click', () => {
        if (!flagsDialog.open) {
            flagsDialog.showModal();
        }
    });
};

docFullyReady().then(() => {
    const app = firebase.app();
    console.log('Features', ['auth', 'database', 'messaging', 'storage'].filter(feature => typeof app[feature] === 'function'));
}).then(() => easyAuth()
).then(user => {
    uId = user.uid;
    return mostRecentDeviceId();
}).then(latestDeviceId => {
    // Setup the form with a single-shot query
    deviceId = latestDeviceId;
    return new Promise(resolve => {
        dbRef = firebase.database().ref(`/users/${uId}/devices/${deviceId}/state`);
        dbRef.once('value', snapshot => resolve(snapshot.val()));
    });
}).then(state => {
    console.info('Initial State for rendering form:', state);
    Object.keys(state).forEach(key => {
        addBidi(key, state[key]);
    });
    instrumentConfigDialog();

    // Fixed page on mobile (along with CSS body position: fixed;
    document.body.ontouchstart  = e => e.preventDefault();
    document.body.ontouchmove  = e => e.preventDefault();

});

