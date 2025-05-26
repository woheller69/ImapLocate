# ImapLocate
By leveraging existing IMAP infrastructure, **ImapLocate** provides a decentralized, protocol-driven approach to synchronizing device location data, avoiding reliance on third-party tracking services.

Inspired by the original *ImapNotes3* project (which stored notes in an IMAP "Notes" folder), this app repurposes the concept to store GPS coordinates in an IMAP folder named "Location" by default. It uses Android’s GPS location services to determine the device’s current coordinates and synchronizes them to the IMAP account under the following conditions:
- **Time-based**: Sync occurs if **30+ minutes** have passed since the last sync, regardless of movement.
- **Hybrid time/distance**:
     - Sync if **15+ minutes** have passed **and** the device has moved **>30 meters** since the last sync.
     - Sync if **5+ minutes** have passed **and** the device has moved **>100 meters**.

This logic balances battery efficiency with location accuracy, ensuring updates during both prolonged inactivity (e.g., stationary devices) and significant movement.

## **Origins & Licensing**
This project is forked from [ImapNotes3](https://github.com/niendo1/ImapNotes3) and incorporates code from [GPS Cockpit](https://github.com/woheller69/gpscockpit), both published under the **GNU GPL v3.0** license.

