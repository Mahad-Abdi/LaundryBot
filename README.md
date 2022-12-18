# LaundryBot
## Overview of Our Project
LaundryBot is a text bot that provides laundry machine availability to Boston College students. It informs them of the availability of washing machines in their residential hall as well as provides reminders of when a washer will be available and when their laundry cycle is done. 

## Running Instruction of the project
To use LaundryBot, you'll need to have a Twilio account with a valid ID and auth token. Once you have these, simply add them to lines 16 and 17 of the LaundryBot.java file located in src/main/java. Then, run the main method of LaundryBot.java and send a text message to "+14782495460" to get started. If you are running this for grading purposes, for convenience please email abdima@bc.edu so that you can use their id and auth token. 

## Overview of Our Design
Our design for LaundryBot includes two main components: the communication aspect, which is handled using the Twilio API for sending and receiving SMS messages, and the information aspect, which is handled using the laundryview.com API to gather data on laundry machine availability. For more details on our design, please see the attached report, which includes additional information to help you understand how the program works.
