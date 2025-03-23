File Management API with Spring Boot

Dependencies:
Spring Web
Spring Data JPA 
MySQL Driver
Lombok

The properties file
- We write the normal configuration to connect to MySQL
- Define the files directory name
- Max file size
- File types supported	

We need an entity to save files' metadata
```java

```

A repository to interact with the database
```java

```

Write the business logic in our service layer
```java

```

Implement the controller to handle user requests 
```java

```

Run application

To upload file. In insomnia (or Postman) configure a POST requests
Add body > Form Data 
Name it "file" and load a document, image, etc.
![configure upload request image]()

Responses
upload file
![upload file image]()

get metadata
![get metadata image]()

download
![download file image]()

delete file
![delete file image]()
