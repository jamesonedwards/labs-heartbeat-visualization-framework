labs-heartbeat-visualization-framework
======================================
24hrs of Tweets

Conversations that occur on-line are constantly ebbing and flowing streams of data that can be disseminated down in a whole variety of ways for visual representation.For us in LABS we knew that the conversation around specific topics was going to be distinctly unique day-to-day and activity was going to be effected based on or around specific events that may spark deeper interest in topics. These spikes in interest were something we wanted to focus in on with our initial phase of exploring data visualization. 

Our goal was to create a visualization that would amount to a unique piece of artwork,created from the data trawled from conversations on twitter. When looking for conversations we focused in on distinct hashtags that were commonly used around distinct events. In our case we started off with Apples WWDC, a reliable source of heavy conversation around new apple products. We pulled the tweets for the entire day for #wwdc2013 which gave us a period not only of the actual keynote, but also of a period of time before and after. With that information collected we set about determining how we would want to visualize the day of tweets.

Our first factor to consider was the timeframe. We would be pulling the tweets from 12 midnight to 12 midnight of the day these events occurred. Taking the metaphor of a clock we decided that the tweets would be represented in a circle representing the 24 hours of the day. The timestamp of the tweets would determine where they fall on the circle. 

Our next factor to consider was the originators of the tweets themselves. Every user within twitter has a number of data points associated with their profile. Followers, following, even the link or background color they have chosen in their profile settings. For us the most compelling was to determine the size of the node that would represent their tweet being larger based on the number of followers they have. This visually conveys influence by the amount of focus they take up in the visual. Colors of the nodes were based off of the background colors chosen by the users after finding that the most variety in colors occurs in the backgrounds as opposed to link or text settings. 

Additional to the size of the node being determined by followers we also added a further layer adding to the size of the node determined by the amount of retweets that particular tweet had received represented by an extended halo of the node. Additionally retweets would be visually connected to any retweeted instance of the original tweet by a connecting arc.

This application was designed as independent, modular components split into three application layers. Together, these layers form an extensible and reusable data visualization framework. Descriptions are as follows:

The Data Collection Layer: responsible for gathering the source data (in our case, Twitter data) and converting it into the common format used throughout the rest of the framework. The REST Services Layer: responsible for data aggregation, query processing, request caching and interfacing with Flickr for storage in the image gallery. The Data Visualization Frontend Layer: responsible for generating the data visualizations and providing the user interface.

Each of these tiers can be independently used, extended and scaled as needed.

The Data Visualization Frontend Layer was built in Java, using the Processing libraries (and some Swing components) for the data rendering and user interface. The REST Services Layer was built in Django and MongoDB, using the Flickr API to save generated images to our Flickr stream. The Data Collection Layer was built in Django, using the Twitter API for data collection.

The biggest challenge we encountered by far was handling the amount of data we received for certain hashtags; in some cases, over 100k data points. This caused problems with Twitters API quota (averted with a wait-retry loop), technical and visual problems displaying the data, and general issues with performance. In the end, we found it best to institute a data cap to prevent information overload (both to the framework and to the user).

Another challenge was designing the framework itself, which had to be flexible enough to extend to multiple data sets and multiple visualizations, yet simple enough to quickly implement our test case (Twitter data with the dots and loops visualization). This challenge was overcome by segmenting the workflow into the layers mentioned, and choosing what we felt were the best tools for each individual task.

The end result is an application that can gather the necessary data to create a unique visualization for any hashtag the user chooses to input. Additionally the background color of the visualization can be user selected to allow even more customization with the end result being an image the user can email or share.

Image samples can be found here: http://www.flickr.com/photos/mcgarrybowenlabs/sets/72157634595405310/
