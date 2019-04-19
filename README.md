# Polls for Nearby Devices

Introduction
------------

This app allows a user to publish a poll to nearby devices, and subscribe
to answers published by those devices(which are publishing the answers after
subscribing to the poll).

To run this sample, use two or more devices to publish and subscribe polls.


Getting Started
---------------

This project was developed in 2 parts:

- 1st part was developed using Nearby Connections API of older version and requires internet connection. This part is now redundant and not required. So can be deleted.
This part includes:
    * `first_page_activity.java`
    * `start_poll_activity.java`
    * `get_poll_activity.java`
    * `DeviceMessage.java`
    
    To use this part: 
  1. Create a project on https://console.developers.google.com/
  2. There: APIs & Services -> Dashboard -> New Project -> Create
  3. From the API Library, select Nearby Messages API -> Enable
  4. Now Create Credentials for it.
  5. Pick the `Android key`. Then register your Android app's SHA1 certificate fingerprint and package name for your app.
  6. Copy the API key generated, and paste it in `AndroidManifest.xml`.

- 2nd part was developed using the latest version of the API, and does not require any internet connectivity and also has additional features.
This part includes:
    * `connections_activity.java`
    * `start_poll_2_activity.java`
    * `get_poll_2_activity.java`
    * `Endpoint.java`


To Be Noted
-----------

I have developed this project to be integrated in another app, which has a login page. So, I am using credentials from that login page to differentiate answers from 2 different people after the integration. You may use one of the options:
 1. Make your own login page and save the login id as a shared preference and then append it with the solution that is sent.
 1. Make an `EditText` field in get_poll.xml and ask the user to input his/her name/id/roll_no there and then append it with the solution.
 1. Append the Device model name with the solution.
    
Note that you must append at the end of the solution. As these solutions are being sorted at the Asker's side, based on the option number. Thus, 2 people with same solution will have their names one above the other.


You can also send multiple solutions. But these solutions will have to be sent one at a time. For instance, if you want to send option 1 and option 3, then, first select option 1 and click the send button, then do the same with option 3.


I have used `disconnectFromAllEndpoints()` function in `start_poll_2_activity`, which I do not think as necessary. This consumes time establishing the connections again. I have done because of the change in strategy from `P2P_STAR` to `P2P_CLUSTER`, when changing from pub to sub. I have done this just to be safe. You may edit it. Same is true for `stopAllEndpoints()` in `connections_activity.java`.


Future Work
-----------

At present, you can only send one question at a time and also only MCQ type questions. I plan to add a QUIZ mode, where you can send multiple questions with `EditText` type of answers.

