import subprocess

print("Running mvn clean compile in shared...")
res = subprocess.run(["mvn", "clean", "compile", "-e"], cwd=r"c:\Dev_Uni\Online-auction-platform\shared", shell=True, capture_output=True, text=True)

with open("compile_out.txt", "w") as f:
    f.write(res.stdout)
    f.write("\n---\n")
    f.write(res.stderr)
print("Done writing to compile_out.txt")
