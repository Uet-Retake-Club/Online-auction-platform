import os
import subprocess

def add_spotless_to_pom(pom_path):
    with open(pom_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if "spotless-maven-plugin" in content:
        return
        
    plugin_xml = """
            <plugin>
                <groupId>com.diffplug.spotless</groupId>
                <artifactId>spotless-maven-plugin</artifactId>
                <version>2.35.0</version>
                <configuration>
                    <java>
                        <googleJavaFormat>
                            <version>1.17.0</version>
                            <style>GOOGLE</style>
                        </googleJavaFormat>
                    </java>
                </configuration>
            </plugin>
"""
    # Insert before </plugins>
    # Wait, shared/pom.xml has NO <build><plugins> section.
    # It has <dependencies>.
    if "<build>" not in content:
        content = content.replace("</project>", "    <build>\n        <plugins>" + plugin_xml + "</plugins>\n    </build>\n</project>")
    else:
        content = content.replace("</plugins>", plugin_xml + "</plugins>")
        
    with open(pom_path, 'w', encoding='utf-8') as f:
        f.write(content)

shared_pom = r"c:\Dev_Uni\Online-auction-platform\shared\pom.xml"
add_spotless_to_pom(shared_pom)

# Run spotless
print("Running spotless...")
res = subprocess.run(["mvn", "spotless:apply"], cwd=r"c:\Dev_Uni\Online-auction-platform\shared", shell=True, capture_output=True, text=True)
print(res.stdout)
print(res.stderr)

# Now check checkstyle
print("Running checkstyle...")
res = subprocess.run(["mvn", "checkstyle:check"], cwd=r"c:\Dev_Uni\Online-auction-platform\shared", shell=True, capture_output=True, text=True)
print(res.stdout)
print(res.stderr)
