require 'rexml/document'

# This script creates jsafran.jar including all libs
# You first have to build the "light" version of jsafran.jar (without libs) with "ant jar" to compile all classes

xml_data = File.read("build.xml")
doc = REXML::Document.new(xml_data)

libs=Array.new
doc.elements.each("project/path") do |e|
  if e.attributes["id"]=="jsafran.classpath"
    e.elements.each("pathelement") do |ee|
      libs.push ee.attributes["location"].gsub '${libs}','lib'
    end
  end
end

puts "expanding all libs into build/ ..."
libs.each do |l|
  puts `cd build; jar xvf ../#{l}`
end

puts "making jar"
s="Manifest-Version: 1.0\n"+
	"Ant-Version: Apache Ant 1.9.3\n"+
	"Created-By: 1.8.0_11-b12 (Oracle Corporation)\n"+
	"Main-Class: utils.installer.JSafranInstall\n"+
	"Implementation-Title: JSafran\n"+
	"Implementation-Version: 1.2\n"+
	"Implementation-Vendor: Christophe Cerisara\n"+
	"Build-Date: ${TODAY}\n"+
	"Class-Path:"
File.write('MANIFEST.MF',s)
puts `jar cvfm jsafran.jar MANIFEST.MF -C build/ .`

puts "building resources..."
puts `jar cvf res.jar *.mco mate.mods* *.cfg tagger lib LICENSE README.*`
puts `scp res.jar talc1:/var/www/users/cerisara/jsafran/res.jar`

