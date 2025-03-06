# botica-seed-java
Template project to facilitate implementing, compiling, and building Botica bots using Maven and [botica-lib-java](https://github.com/isa-group/botica-lib-java/).

## Usage

1. [Create a repository based on this template](https://github.com/new?template_name=botica-seed-java&template_owner=isa-group).

2. Modify the `pom.xml` file, specifically:
   1. The `<groupId>` and `<artifactId>` tags.
   2. The `<mainClass>` property, pointing to your launcher class. This assumes that you renamed the template classes or packages in `src/`.
   3. The `<imageTag>` property. The build script will take the tag for the resulting Docker image from this property.

3. Implement your bot's logic. You can follow one of [these examples](./src/main/java/com/example/examples).
  > [!NOTE]
  > Full project examples are also available, with their respective Java implementations. Check out [botica-infrastructure-fishbowl](https://github.com/isa-group/botica-infrastructure-fishbowl) or [botica-infrastructure-restest](https://github.com/isa-group/botica-infrastructure-restest).

4. Run the build script. This script compiles the Maven project and builds the Docker image for you based on the `<imageTag>` property in `pom.xml`. The `maven-assembly-plugin` will include your dependencies in the compiled JAR.
   1. For Linux or macOS systems, run `./build.sh` in your IDE's terminal.
   2. For Windows systems, run `build.bat` in your IDE's terminal.
