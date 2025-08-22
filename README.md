<pre>Send a coffee to 
woheller69@t-online.de 
<a href= "https://www.paypal.com/signin"><img  align="left" src="https://www.paypalobjects.com/webstatic/de_DE/i/de-pp-logo-150px.png"></a>

  
Or via this link (with fees)
<a href="https://www.paypal.com/donate?hosted_button_id=XVXQ54LBLZ4AA"><img  align="left" src="https://img.shields.io/badge/Donate%20with%20Debit%20or%20Credit%20Card-002991?style=plastic"></a></pre>


# ImapLocate
By leveraging existing IMAP infrastructure, **ImapLocate** provides a decentralized, protocol-driven approach to synchronizing device location data, avoiding reliance on third-party tracking services.

Inspired by the original *ImapNotes3* project (which stored notes in an IMAP "Notes" folder), this app repurposes the concept to store GPS coordinates in an IMAP folder named "Location". It uses Android’s GPS location services to determine the device’s current coordinates and synchronizes them to the IMAP account under the following conditions:
- **Time-based**: Sync occurs if **30+ minutes** have passed since the last sync, regardless of movement.
- **Hybrid time/distance**:
     - Sync if **15+ minutes** have passed **and** the device has moved **>30 meters** since the last sync.
     - Sync if **5+ minutes** have passed **and** the device has moved **>100 meters**.

This logic balances battery efficiency with location accuracy, ensuring updates during both prolonged inactivity (e.g., stationary devices) and significant movement.

[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" height="75">](https://apt.izzysoft.de/packages/org.woheller69.ImapLocate)

## **Origins & Licensing**
This project is forked from [ImapNotes3](https://github.com/niendo1/ImapNotes3) and incorporates code from [GPS Cockpit](https://github.com/woheller69/gpscockpit), both published under the **GNU GPL v3.0** license.
ImapLocate is published under **GNU GPL v3.0** license.

