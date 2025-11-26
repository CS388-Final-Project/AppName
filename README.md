# Milestone 1 - MusicMedia (Unit 7)

## Table of Contents

1. [Overview](#Overview)
1. [Product Spec](#Product-Spec)
1. [Wireframes](#Wireframes)

## Overview

### Description

###  Description
The goal of this app is to connect people through music. The main feature of the app will be a time initiated notification for the user to take a picture. This notification will route the user to a camera and have their picture taken. After this picture is taken the post will be linked with the song that the user is currently listening to and it will have their location listed. This will then be uploaded to a feed that can be seen by other users that the current user is friends with. The current user will be able to see a feed of their friends and people that they are following with the same format for the picture/song/location. At the end of each week the most listened to songs/artists on a user's feed will be shown to them in a list.

### App Evaluation


- **Category: Social**
- **Mobile:** Uses camera, location, audio playback, and push notifications
- **Story:** Provides a clear value to users: connect through music, discover what friends are listening to, and share moments linked with songs
- **Market:** The market for this app is very large but mainly targets younger users from ages 18-30 but will hopefully be used by people from all age demographics. This app will be aiming to help people of ages to connect with each
- **Habit:** Daily/weekly usage expected. Users will regularly receive notifications, post pictures, and check their feed
- **Scope:** Core functionality is feasible for an MVP (camera + song linking + feed) and advanced features (analytics, trends, gamification) can be added later if needed

## Product Spec

### 1. User Features (Required and Optional)

**Required Features**

1. allow the user to take a picture when prompted by a notification
2. Automatically link the current song the user is listening to with the post
3. Tag the post with the user’s current location
4. Upload the post to the user’s feed and make it visible to friends/followers
5. View a feed of friends’ and followed users’ posts
6. Weekly summary showing most listened-to songs/artists from the user’s feed

**Optional Features**

1. Like or comment on posts in the feed
2. Map view of posts based on location to explore music trends geographically
3. Push notifications for friends’ activity or trending songs
4. Profile customization with bio, favorite genres, and playlist highlights

### 2. Screen Archetypes

- Signup/Login Screen
  - The user will be prompted with a sign up or login screen. First time users will be expected to sign up with a username, email, and password. The user should be able to stay logged in if they are acessing from a device that was recently used to access the account. 
- User Picture Screen
  - The user should be routed to this screen if the notification for a post has been sent for a day and the user hasn't made a post yet. This should only be shown the user if they haven't made their post yet for the day and users should not access it multiple times a day.
- User Feed
    - The user should be able to access this screen only after they have made their post for the day. This should show the user all of the posts made by other users on the app and it should allow them to scroll through and look at each post. 
- User Recommendations
    - This should be another screen that shows the users the top listened to artists/songs from their feed and shows them in a list. 

### 3. Navigation

**Tab Navigation** (Tab to Screen)

* Feed Tab: The users feed should be a tab on the bottom that takes the user to their feed page.
* Camera Tab: This should only be accessed if the user has not posted for the day
* Recommendation Tab: The user recommendations tab should be the furthest tab from the user and it should link to the page showing the list of user recommendations
*  Friends Page: There should be a friends page where a user can search usernames to add people or there will be a recommendation of who they should add based on their current friends and this page can also store who added the user and allow them to add them back
* Profile Page: This should show the user their music listening stats and what artists/songs they are listening to the most and how many days in a row they have posted


**Flow Navigation** (Screen to Screen)

- Signup/Login -> Feed or Camera
  - If the user hasn't made their post for the day yet they will be directed straight to the camera
  - If they have made their post for that day it will direct them straight to the feed where they can start scrolling. 
- Feed -> User Profile
  - This will allow the user to click on a post by another user and from there click on their name and it will link them to that person's profile.
- Camera -> Feed
    - After the user takes their picture for the day and uploads it, they should be routed to the feed page where they can see other posts.

## Wireframes

[Add picture of your hand sketched wireframes in this section] <img src="https://github.com/user-attachments/assets/4160ad8b-da04-4225-9bd8-a38ccafc3ec8" width=600>




<br>

<br>

### [BONUS] Digital Wireframes & Mockups

### [BONUS] Interactive Prototype

<br>

# Milestone 2 - Build Sprint 1 (Unit 8)

## GitHub Project board

[Add screenshot of your Project Board with three milestones visible in
this section]
<img width="1800" height="583" alt="MilestoneSS" src="https://github.com/user-attachments/assets/d1428918-8d03-4531-8ed5-6bfb34ef7825" />


## Issue cards
<img width="1895" height="902" alt="Sprint1SS" src="https://github.com/user-attachments/assets/2022975c-e234-43fe-a233-da2614b974b2" />


<img width="1897" height="911" alt="Sprint2SS" src="https://github.com/user-attachments/assets/75713be6-570e-4e6d-98da-eea3351828cf" />


## Issues worked on this sprint

- Project Setup: This was the general setup of the project that would allow us to move forward cleanly and smoothly. This was setting up Android Studio and getting the necessary permissions and dependencies that we will at least need to move forward with the project and making sure that it has the proper functionality.
- App Navigation: This was adding a Navigation bar to our app that would allow for users to transition between pages on the app easily.
- Database Setup: For this we set up a firebase and room database taht would allow for us to take in certain data that we would need to save from the user such as a username and a password that they sign up with that will allow them to login to their same account later.
- User Authentication: This was the process of setting up user sign up and login and making sure that users were able to make an account and then log in to that same account later on using their password.

- [Add giphy that shows current build progress for Milestone 2. Note: We will be looking for progression of work between Milestone 2 and 3. Make sure your giphys are not duplicated and clearly show the change from Sprint 1 to 2.]

# Signing Up Demo
https://github.com/user-attachments/assets/2bbb54a6-06ad-4ec6-ab68-26c5c6121866

# Logging In Demo
https://github.com/user-attachments/assets/646251b9-2fd3-4617-ab13-c500a814408b

<br>

# Milestone 3 - Build Sprint 2 (Unit 9)

## GitHub Project board

[Add screenshot of your Project Board with the updated status of issues for Milestone 3. Note that these should include the updated issues you worked on for this sprint and not be a duplicate of Milestone 2 Project board.] <img width="1402" height="881" alt="image" src="https://github.com/user-attachments/assets/37e79a65-1498-4bf9-b728-5f054eff3f59" />

## Completed user stories

- List the completed user stories from this unit
- List any pending user stories / any user stories you decided to cut
from the original requirements

[Add video/gif of your current application that shows build progress]
<div>
    <a href="https://www.loom.com/share/6485beb20f084922bbe83c235d5a288f">
      <p>Introducing Music Media: A Social Platform Connecting People Through Music - Watch Video</p>
    </a>
    <a href="https://www.loom.com/share/6485beb20f084922bbe83c235d5a288f">
      <img style="max-width:300px;" src="https://cdn.loom.com/sessions/thumbnails/6485beb20f084922bbe83c235d5a288f-6ac4e9234eaa77ff-full-play.gif#t=0.1">
    </a>
  </div>
## App Demo Video
https://youtu.be/yMk2amPZAe0


