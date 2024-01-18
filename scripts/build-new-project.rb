require 'optparse'
require 'fileutils'

# Default values
repo_url = "https://github.com/3sidedcube/sprintzero-template-android-client.git"
local_temp_directory = "sprintzero-template-android-client"
new_package = ""
new_name = ""
gs_directory = ""

# Function to update the main package structure
def replace_package_directory(new_package, directory)
    source = "./app/src/#{directory}/java/com/cube/sprintzerotemplate/."
    temp_destination = "./app/src/#{directory}/java/sprintzerotemplate"
    final_destination = "./app/src/#{directory}/java/" + new_package

    # Copy the source directory to the temp destination
    FileUtils.cp_r(source, temp_destination)

    # Cleanup old directories
    FileUtils.remove_entry("./app/src/#{directory}/java/com")

    # Create the new final destination directory if it doesn't exist
    FileUtils.mkdir_p(final_destination)

    # Copy the source directory to the destination
    FileUtils.cp_r(temp_destination + "/.", final_destination)

    # Cleanup temp directories
    FileUtils.remove_entry(temp_destination)
end

# Function to replace all instances of com.cube.sprintzerotemplate
def find_and_replace(search_string, replacement_string)
    Dir.glob(File.join("./", '**', '*.{java,xml,gradle,kt}')) do |file|
        next unless File.file?(file)

        content = File.read(file)
        new_content = content.gsub(search_string, replacement_string)

        if content != new_content
            File.write(file, new_content)
        end
    end
end

# Setup command line options
OptionParser.new do |opts|
    opts.banner = "Usage: ruby script.rb [options] -n PROJECT_NAME"

    opts.on("-p", "--package-name PACKAGE_NAME", "New package name (required)") do |package|
        new_package = package
    end

    opts.on("-n", "--project-name PROJECT_NAME", "New project name (required)") do |name|
        new_name = name
    end

    opts.on("-g", "--gs-path GOOGLE_SERVICES_PATH", "Path to google-services.json (required)") do |path|
        gs_directory = path
    end

    opts.on_tail("-h", "--help", "Show this message") do
        puts opts
        exit
    end
end.parse!

############# MAIN ENTRY #############

# Check if the new package is provided
if new_package.empty?
    puts "Error: New package name is required. Use -p or --package-name option to specify the new package name"
    exit 1
elsif !new_package.match?(/\A[a-z]+(\/[a-z]+)*\z/)
    puts "Error: Invalid package name format. Please use a string in the format 'com/cube/whatever'"
    exit 1
end

# Check if the new name is provided
pattern = /\A[A-Z][a-z]*(?:[a-z]+[A-Z][a-z]*)*\z/
if new_name.empty?
    puts "Error: New project name is required. Use -n or --project-name option to specify the new name"
    exit 1
elsif !new_name.match?(pattern)
    puts "Error: Invalid project name format. Please use a string in the format 'MyNewProjectName'"
    exit 1
end

# Check if the google services file is provided
if gs_directory.empty? and File.file?(gs_directory)
    puts "Error: New project name is required. Use -n or --project-name option to specify the new name"
    exit 1
end

# Clone the repo
if Dir.exist?(local_temp_directory)
    Dir.chdir(local_temp_directory) do
        puts "Error: Temp directory already exists"
        exit 1
    end
else
    system("git clone #{repo_url} #{local_temp_directory} --quiet")
end

# Perform Configuration
Dir.chdir(local_temp_directory) do
    system("git checkout develop --quiet")

    # Remove Git
    if File.directory?(".git")
        FileUtils.remove_entry(".git")
    end

    # Remove Script from clone
    if File.file?("./scripts/build-new-project.rb")
        FileUtils.remove_entry("./scripts/build-new-project.rb")
    end

    # Update package declarations
    find_and_replace("com.cube.sprintzerotemplate", new_package.gsub("/", "."))

    # Update application class and manifest
    find_and_replace("class SprintZeroTemplateApp", "class #{new_name}App")
    FileUtils.mv("./app/src/main/java/com/cube/sprintzerotemplate/app/SprintZeroTemplateApp.kt", "./app/src/main/java/com/cube/sprintzerotemplate/app/#{new_name}App.kt")
    find_and_replace("android:name=\".app.SprintZeroTemplateApp\"", "android:name=\".app.#{new_name}App\"")
    find_and_replace("android:theme=\"@style/Theme.SprintZeroTemplate\"", "android:theme=\"@style/Theme.#{new_name}\"")
    find_and_replace("android:theme=\"@style/Theme.SprintZeroTemplate.Splash\"", "android:theme=\"@style/Theme.#{new_name}.Splash\"")

    # Update settings.gradle
    find_and_replace("rootProject.name = \"Sprint Zero Template\"", "rootProject.name = \"#{new_name}\"")

    # Update bitrise.yml
    find_and_replace("OUTPUT_NAME: sprintzerotemplate-app-release-firebaseStaging-apiStaging-signed", "OUTPUT_NAME: #{new_name.downcase}-app-release-firebaseStaging-apiStaging-signed")
    find_and_replace("OUTPUT_NAME: sprintzerotemplate-app-release-firebaseLive-apiLive-signed", "OUTPUT_NAME: #{new_name.downcase}-app-release-firebaseLive-apiLive-signed")

    # Update strings.xml and themes.xml
    find_and_replace("<string name=\"app_name\">Sprint Zero Template</string>", "<string name=\"app_name\">#{new_name}</string>")
    find_and_replace("<style name=\"Theme.SprintZeroTemplate.Splash\" parent=\"Theme.SplashScreen.IconBackground\">", "<style name=\"Theme.#{new_name}.Splash\" parent=\"Theme.SplashScreen.IconBackground\">")
    find_and_replace("<style name=\"Theme.SprintZeroTemplate\" parent=\"Theme.MaterialComponents.Light.NoActionBar\">", "<style name=\"Theme.#{new_name}\" parent=\"Theme.MaterialComponents.Light.NoActionBar\">")
    find_and_replace("<item name=\"postSplashScreenTheme\">@style/Theme.SprintZeroTemplate</item>", "<item name=\"postSplashScreenTheme\">@style/Theme.#{new_name}</item>")

    # Update the package structure
    replace_package_directory(new_package, "main")
    replace_package_directory(new_package, "test")
    replace_package_directory(new_package, "androidTest")
end

# Copy the source directory to the destination
FileUtils.cp_r("#{local_temp_directory}/.", ".")

# Remove the source directory
if File.directory?(local_temp_directory)
    FileUtils.remove_entry(local_temp_directory)
end

# Copy google-services.json
unless Dir.exist?("./app/src/firebaseStaging/")
    FileUtils.mkdir_p("./app/src/firebaseStaging/")
end

if File.file?(gs_directory)
    FileUtils.mv(gs_directory, File.join("./app/src/firebaseStaging/", File.basename(gs_directory)))
end

puts "Project was created successfully!"

FileUtils.rm(File.expand_path(__FILE__))
