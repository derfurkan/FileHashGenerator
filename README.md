# ⚙ FileHashGenerator
### 🔨 Java 17 was used to build this application.
#

FileHashGenerator is a program that will generate file hashes out of a folder. These hashes will be put into an output file.
It uses thread technology to get the maximum speed out of any system to generate hashes as quickly as possible.

## How to run
FileHashGenerator does not need any arguments.<br/>
The run command would look like this<br/>
`java -jar FileHashGenerator.jar`<br/>
<br/>
### Here are the questions you will encounter while running the application

* `Should we include sub-folders Y/N` Specifies if hashes also should be generated for the files in subfolders.
* `Please specify an algorithm`
You can choose between `MD5, SHA1, SHA256, SHA512, ADLER32, CRC32`
> Where [`ADLER32`](https://en.wikipedia.org/wiki/Adler-32#Calculation "ADLER32") and [`MD5`](https://en.wikipedia.org/wiki/MD5#Algorithm "MD5") are checksum algorithms and therefor the fastest option.
* `Please specify a outputFile` Here you need to put a Path with a file on the end like
`C:\Users\USER\hashes.json`
* `Do you want to use multithreading Y/N` Specifies if the generator should work with multiple threads.<br/> _(This option will result in much faster generation time but also a higher CPU load.)_
* `How many threads should be used` Number of threads that should be active at once for faster generation.

</p>

`outputFile` example: _(Can be checked with [FileHashChecker](https://github.com/derfurkan/FileHashChecker "FileHashChecker"))_
```Json
{
  "\\Folder1\\File1.txt": "d41d8cd98f00b204e9800998ecf8427e",
  "\\File2.txt": "d41d8cd98f00b204e9800998ecf8427e",
  "\\Folder1\\Folder2\\File1.txt": "d41d8cd98f00b204e9800998ecf8427e",
  "\\File3.txt": "d41d8cd98f00b204e9800998ecf8427e",
  "\\File5.txt": "d41d8cd98f00b204e9800998ecf8427e"
}
```

## How it works

FileHashGenerator will generate hashes of files in the folder where the jar is located.<br/>
You will be asked a few questions before generating.<br/>
It will first locate all files in the folder and put them in a list and sort them by file size for a higher quantity of generated hashes in a short time.<br/>
After that it will split the list into the `threads` number and put the rest in an extra thread.<br/>
Now the MainThread will post a status every second in the command prompt.









