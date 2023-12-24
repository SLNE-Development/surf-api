job("Build and publish") {
   container("Run gradle build", "eclipse-temurin:17") {
       env["REPOSITORY_URL"] = "https://packages.slne.dev/maven/p/surf/maven"
       kotlinScript {api ->
           val env = System.getenv()
           val REPOSITORY_URL by env
           val JB_SPACE_CLIENT_ID by env
           val JB_SPACE_CLIENT_SECRET by env

           println("REPOSITORY_URL: $REPOSITORY_URL")
              println("JB_SPACE_CLIENT_ID: $JB_SPACE_CLIENT_ID")
                println("JB_SPACE_CLIENT_SECRET: $JB_SPACE_CLIENT_SECRET")

           api.gradlew("shadowJar", "--stacktrace")
           api.gradlew("publish", "--stacktrace", "-DrepoUrl=$REPOSITORY_URL", "-Dusername=$JB_SPACE_CLIENT_ID", "-Dpassword=$JB_SPACE_CLIENT_SECRET")
       }
   }
}