#  Activity 1: App Idea Brainstorming

---
##  Table of Contents

1.) [Generate New Ideas](#GenerateNewIdeas)
    
2.1) [Select the Top 3](#SelectTheTop3)

2.2) [Evaluate Top 3](#EvaulateTop3)

3) [Final Selection](#FinalSelection)


---

##  Team

| Name | Role (optional) | GitHub | Email |
|------|------------------|--------|-------|
| Keegan Wooding | Developer/Designer | @kwooding | krw6@njit.edu |
| Michael McGillycuddy | Developer/Designer | @mikemcg33 | mkm@njit.edu |
| Hegel Guevara | Developer/Designer | @HGuevara10 | hg326@njit.edu |

---

##  Step 1: Generate New Ideas 

### Member: Keegan Wooding
1. **Grocery Helper** — This app will be used by everday people who want to speed up the process of shopping. This allows the user to be able to enter the items on their grocery list and then choose a store of their choosing then the app will show them their entire list sorted in aisle order so users can do their shopping in one go.
2. **Commute Helper** — This app will be used by people everyday who need to time out their commute to get to their destination on time. This would communicate with a map API and parking API that would be able to let the user know exactly when they need to leave to arrive on time.
3. **Playlist Creator** — This will allow the users to be able to find music that they want to listen to. It will let the user listen to a 15-30 second clip of the most replayed part of the song. The user can then swipe right to add it to a playlist and will swipe left if they are not interested in the song. The non selected songs will be put into a database that will not be shown to the user again unless specifically prompted. The users can select which music category they like or they can have it based off of their top listened to songs/artists. This can use the spotify API. It can also show the user concerts in their area and they can select which ones they are most interested in. This could also have a group option where users on other devices can swipe which songs they want to add to a group playlist that could play in the order that they were added in.

### Member: Michael McGillycuddy
1. **Music Media** — This app acts as a social media app, centered on music where users upload an image of themselves throughout the day, and compliment it with a song the they feel fits the mood of that day. It involves location data which ultimately compiles the top songs that people on the platform are listening to that day, and the top songs in your area. It gives you the option to look at more features about popular songs so that users can get more information so users can decide whether they want to add it to their playlist.
2. **Routine Router"** — This app takes in user data regarding daily routines each day throughout the week, and stores the data in a database. The app then compiles an optimized daily routine, to help the user visualize what their "perfect day" looks like. The app uses API data to compare user habits with general habits and also logs these habits for users to refer to.
3. **Study Buddy** — This app is meant to help users study more efficiently by allowing them to choose the topic/subject they are studying for, and the app compiles study resources including video links, article links, and other helpful tools that the user can use for their own desires. It also features a rating system in which users can rate the resources and subsequently make it so that higher rated resources are prioritized.

### Member: Hegel Guevara
1. **Leftover Ingredients App** — User inputs a list of ingredients that they already have and the app will give them a list of foods that they can make along with the exact recipies to follow. 
2. **Wikipedia Locations Travel App** — App that gives photos, historical facts, and current weather reports on a location. The user can swipe through locations like a dating app so they can build a bucket list of locations they would like to visit. 
3. **Cookbook Universe** — Fictional cookbook generator based on movies and games. "The Hobbit's Second Breakfast Guide," "Final Fantasy Potions IRL," "Breaking Bad's Los Pollos Hermanos Menu." Creates real, tested recipes inspired by fictional universes.
4. **Recipe Roulette** — Spin a virtual globe to land on random countries, then get authentic recipes with stunning food photography, ingredient translations, and cultural Wikipedia entries about that cuisine's history. 

##  Step 2.1: Select the Top Three

> Quick vote: each member gets **3 votes** 

| Idea | Votes (✓) | Voters |
|------|-----------|--------|
| Grocery Helper | | |
| Commute Helper |  | |
| Playlist Creator | ✓✓✓ | Keegan, Michael, Hegel |
| Music Media | ✓✓✓ | Keegan, Michael, Hegel|
| Routine Router | ✓ | Michael |
| Study Buddy |  | |
| Leftover Ingredients App |  | |
| Wikipedia Locations Travel App | ✓✓ | Keegan, Hegel |
| Cookbook Universe |  |  |
| Recipe Roulette |  |  |

**Top 3 selected:** **Playlist Creator · Music Media · Wikipedia Locations Travel App**

---

##  Step 2.2: Evaluate Top 3 

> Rate each idea on **Mobile / Story / Market / Habit / Scope** (1–5), add notes, and check **API availability**.

###  Evaluation Rubric (what to consider)

- **Mobile:** Uniquely mobile (maps, camera, location, audio, sensors, push, real-time)? Try for **2+** native capabilities.
- **Story:** Clear value; compelling to the audience; peers would “get it”.
- **Market:** Size/uniqueness; huge value to a niche; well-defined audience.
- **Habit:** Frequency of use; creation vs consumption.
- **Scope:** Feasible within the course timeline; a stripped-down MVP is still useful; requirements are clear.

---

### Idea #1 — **Playlist Creator**
The idea of this app is to make finding new music fun and engaging.

| Criterion | 1 | 2 | 3 | 4 | 5 | Notes |
|-----------|---|---|---|---|---|------|
| **Mobile** |  | X |  |  | |  | This app uses Audio to display snippets of songs to the user for them to listen to|
| **Story**  | | | | X | | Music is a key experience in life and this app provides ways to find more music |
| **Market** | | | | | X | Music is a worldwide interest and people want new ways to find music |
| **Habit**  | | | | | X | Can be used everyday and users would be on everyday that they want to discover new music |
| **Scope**  | | | X | | | This is feasible to complete within the given time frame for the basic features but adding more complex uses may take too much time |

**API Check (required):**
- Candidate APIs: (https://developer.spotify.com/documentation/web-api), (https://www.last.fm/api)
- **Cost:** Free
- **Limits:** These both have throttling limits that only allow so many requests in a given time frame. Spotify also can have depreciated audio for certain tracks so it is not ideal to use it for every part.
- **Data fit:** These APIs give us information on artist/music, audio features, similar artist and user preferences
- **Feasibility verdict:**  Viable

**Total (out of 25):** **19 / 25**

---

### Idea #2 — **MusicMedia**
**One-liner:** *Users interact over social media to share favorite songs, make it easier to discover music, and to connect with one another online*

| Criterion | 1 | 2 | 3 | 4 | 5 | Notes |
|-----------|---|---|---|---|---|------|
| **Mobile** | ☐ | ☐ | ☐ | ☐ | x | This app uses a camera to get images of users at various parts of the day. It uses location data to access where the user is from and group songs by location. It uses audio to playback music from the platform. Finally, it uses push notifications to notify users of other users activity.  |
| **Story**  | ☐ | ☐ | x | ☐ | ☐ | The value of this app is clear to our audience. It gives music lovers an easy way to connect with others and to discover more music through a social media outlet. We expect people to respond well to this product idea given that social media is so addictive and a music is a factor that is part of a majority of peoples lives. |
| **Market** | ☐ | ☐ | x | ☐ | ☐ | The market for this app is reasonably sized. We would be targeting teens, and people in their 20's-30's who are already consumed by social media. As time goes on we will scale the application to older age groups that are technologically proficient.The app provides value to specifically highschool/college students who are looking to connect with peers through shared interests. |
| **Habit**  | ☐ | ☐ | ☐ | ☐ | x | As does every other social media app, this app will be naturally addictive. Users will frequently use this app to make posts, and to view other peoples posts throughout the day. The average user would be inclined to create on our app by interacting on a day to day basis and adding to the statistics of the application. |
| **Scope**  | ☐ | ☐ | ☐ | ☐ | x | The scope of this app is reasonable. It will be challenging to create this app, however, the core functionality of this app should be pretty straight forward. The complexity of this app comes when we add various features, so a stripped down version would still be interesting. Our goal is to implement as many features of the application  |

**API Check (required):**
- Candidate APIs: (https://developer.spotify.com/documentation/web-api), (https://developers.google.com/maps/documentation/places/web-service)
- **Cost:** Free ☐  Freemium x  Paid ☐
- **Limits:** For the spotify API, the limit is at a rate of about 10 requests a second per user, and requires OAuth 2.0 for most end points. The google maps API allows for about 1000 requests/day and requires an API key from Google Cloud Console.
- **Data fit:** The spotify API provides, song titles, artists, genre, previews of music, playlists, and listening data. This is more than what we are looking for. The google mapes api provides data regarding locations, and cities. It may be a bit challenging accessing user location data.
- **Feasibility verdict:**  Viable

**Total (out of 25):** **21 / 25**

---

### Idea #3 — **Wikipedia Travel Locations**
**One-liner:** *gives photos, historical facts, and current weather reports, and allowing the user to swipe through locations so they can build a bucket list of locations they would like to visit.*

| Criterion | 1 | 2 | 3 | 4 | 5 | Notes |
|-----------|---|---|---|---|---|------|
| **Mobile** | ☐ | x | ☐ | ☐ | ☐ | location, camera |
| **Story**  | ☐ | ☐ | ☐ | x | ☐ | makes discovering different locations easy & accessible for everyone |
| **Market** | ☐ | ☐ | ☐ | x | ☐ | Appeals to travelers, students, and anyone curious about world locations |
| **Habit**  | ☐ | ☐ | x | ☐ | ☐ | Weekly usage; users can explore new places and update their bucket list regularly |
| **Scope**  | ☐ | ☐ | x | ☐ | ☐ | MVP feasible, can start with Wikipedia + weather API; moderate complexity integrating image sources |

**API Check (required):**
- Candidate APIs: https://www.mediawiki.org/wiki/API:Action_API, https://openweathermap.org/api
- **Cost:** Free [x]  Freemium ☐  Paid ☐
- **Limits:** Wikipedia (no auth, generous rate limits); OpenWeatherMap (60 calls/minute on free tier);
- **Data fit:** Yes — provides the core content (facts, weather, and photos) needed for the app’s functionality
- **Feasibility verdict:**  Viable

**Total (out of 25):** **_16_ / 25**

---

##  Step 3: Final Decision

**Music Media**

**Why we chose it:**
- **Mobile:** This app uses a camera to get images of users at various parts of the day. It uses location data to access where the user is from and group songs by location. It uses audio to playback music from the platform. Finally, it uses push notifications to notify users of other users activity. 
- **Story:** The value of this app is clear to our audience. It gives music lovers an easy way to connect with others and to discover more music through a social media outlet. We expect people to respond well to this product idea given that social media is so addictive and a music is a factor that is part of a majority of peoples lives.
- **Market:** The market for this app is reasonably sized. We would be targeting teens, and people in their 20's-30's who are already consumed by social media. As time goes on we will scale the application to older age groups that are technologically proficient. The app provides value to specifically highschool/college students who are looking to connect with peers through shared interests.
- **Habit:** As does every other social media app, this app will be naturally addictive. Users will frequently use this app to make posts, and to view other peoples posts throughout the day. The average user would be inclined to create on our app by interacting on a day to day basis and adding to the statistics of the application.
- **Scope:** The scope of this app is reasonable. It will be challenging to create this app, however, the core functionality of this app should be pretty straight forward. The complexity of this app comes when we add various features, so a stripped down version would still be interesting. Our goal is to implement as many features of the application.

**Risks & Mitigations:**
- Being Able to access the proper API data → Use multiple APIs that perform the same tasks and see which ones perform the best on the tasks that we need. Try to overlap strengths if needed 
- Utilizing/testing sensor data → Use alternative location API's that are more compatible with our application. If it comes to it we will get user data through user input. 

**MVP (Week 1–2):**
- User authentication and profile setup (create account, set username, add profile picture)
- Music post creation (upload or link a song, optionally add a photo and caption)
- Feed display (scoll interface which shows posts from nearby users or global feed)

**Stretch Goals (if time):**
- "Follow Friend" feature to allow you to permanently connect with other users with similiar music tastes. 

- Get more in depth user data for personal history. ie.) Show what artists and songs the user listened to the most and show a pop up weekly to show the user this data

---

> Relevant links below:

-(https://developer.spotify.com/documentation/web-api), 

-(https://developers.google.com/maps/documentation/places/web-service)

---



