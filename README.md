# LaundryBot
## Overview of Our Project
Our project is to use Twilio API to send the updated information to our clients, mainly Boston College students who live on campus. 
We are hoping that by creating such server so our students can avoid the process of waiting for laundry.

## Running Instruction of the project
First ensure that you have a Java run IDE, we used IntelliJ and we recommend that one for those intending to run our projects. Then click open with the github button, then the project will be open in the IDE, then click to start the “run main”, a laundry texting bot will be ready to use. Currently we are only supporting to collect and send laundry information for 10 residential halls, but we are hoping to add more as we are advertising our projects to the students.

## Overview of Our Design
The design consists of two parts: the Twilio API connects to the server by collecting the user's laundry information, and the second part is to make a HTTP GET request to the website that contains the laundry information of the hall. More details of our design is included in a report attached below that we submitted for class, and there are sufficient comments in the file that can help understand the format converting process.
