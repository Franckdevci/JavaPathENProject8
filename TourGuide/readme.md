# TourGuide Project

TourGuide is a recommendation application for tourist sites, attractions, or hotels tailored to individuals based on their exact GPS location.

## 📌 Features

- Personalized recommendations based on the user's GPS position
- Point accumulation system depending on visited places
- Discount offers on hotels and attractions based on earned points

## 🧩 Prebuilt Components

The core of the application was already provided and includes:

- **GPSUtil**: Calculates user location based on latitude and longitude.
- **Reward Central**: Lists attractions, hotels, and points of interest, along with GPS positions and point values.
- **TripPricer**: Calculates the cost of a trip depending on number of people, duration, and destination.

## 🎯 Project Goals

- **Debug** the application by fixing issues detected by existing unit tests.
- **Develop a scoring algorithm** to calculate points based on attractions and places visited by the user.
- **Improve application performance** by implementing asynchronous methods and thread pool management to meet performance benchmarks.
- **Set up a CI/CD pipeline** using **GitHub Actions** to:
  - Run unit tests
  - Build the application
  - Generate a `.jar` file, available in the **Actions** tab of the repository

## 🛠️ Tech Stack

- Java
- Spring Boot
- Spring Web Starter
- JUnit 5
- GitHub Actions

## 🚀 Getting Started

To build and run the project locally:

1. Clone the repository  
   ```bash
   git clone https://github.com/Franckdevci/JavaPathENProject8.git
   cd tourguide
   ```
   
2. How to have gpsUtil, rewardCentral and tripPricer dependencies available ?

> Run : 
- mvn install:install-file -Dfile=/libs/gpsUtil.jar -DgroupId=gpsUtil -DartifactId=gpsUtil -Dversion=1.0.0 -Dpackaging=jar  
- mvn install:install-file -Dfile=/libs/RewardCentral.jar -DgroupId=rewardCentral -DartifactId=rewardCentral -Dversion=1.0.0 -Dpackaging=jar  
- mvn install:install-file -Dfile=/libs/TripPricer.jar -DgroupId=tripPricer -DartifactId=tripPricer -Dversion=1.0.0 -Dpackaging=jar


## 📦 CI

A GitHub Actions workflow is included to:
- Validate tests
- Package the application
- Provide the `.jar` file under the **Actions** tab of the repository

---
