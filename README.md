# Covid19 Telegram Bot for Vaccine Alerts

This repository stores the source code of the Telegram Bot CoWIN Alerts available at https://t.me/covid19_vaccine_tracker_bot

## Features

* Send up to 5 pincodes for updates.
* Set your preferred age, dose and vaccine for alerts.
* Bot checks against CoWIN API every 5 minutes.
* Streaming pipeline to notify as soon as slots open.
* Receive alerts in your local language based on input pincode.

## Architecture

![image](https://user-images.githubusercontent.com/4991449/120941710-6c05c680-c724-11eb-8884-d2156ad2664d.png)


### Design Goals
* Send notifications as soon as new Vaccination slots are available.
* Store critical data (like user requests) in Kafka to achieve RPO = 0
* Store non-critical data (like Vaccine slots which can be recovered from CoWIN API)
 outside Kafka to keep costs low.

## Screenshots

<div>
<img src="https://user-images.githubusercontent.com/4991449/120036469-76c3ab80-c000-11eb-8925-3ce2ba8c8762.jpg" alt="set pincode" width="400"/>
<img src="https://user-images.githubusercontent.com/4991449/120036171-01f07180-c000-11eb-83fc-4a051a15fdb4.jpg" alt="receive alerts" width="400"/>
</div>
