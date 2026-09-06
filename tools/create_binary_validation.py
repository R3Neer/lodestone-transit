"""Create a separate test project that loads the built mod JAR, never main sources."""
from pathlib import Path
import argparse
import json
import shutil

parser = argparse.ArgumentParser()
parser.add_argument('directory', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
target = args.directory.resolve()
assert not target.exists(), 'Use a new validation directory to preserve prior evidence'
version = next(l.split('=',1)[1].strip() for l in (root/'gradle.properties').read_text().splitlines() if l.startswith('version='))
jar = root/'build/libs'/f'lodestone-transit-{version}.jar'
assert jar.is_file(), 'Build the release first'
target.mkdir(parents=True)
shutil.copytree(root/'src/gametest', target/'src/gametest')
shutil.copytree(root/'gradle', target/'gradle')
for name in ('gradlew','gradlew.bat','gradle.properties','settings.gradle'):
    shutil.copyfile(root/name,target/name)
(target/'libs').mkdir()
shutil.copyfile(jar,target/'libs'/jar.name)
build = (root/'build.gradle').read_text()
build = build.replace('repositories { mavenCentral() }', "repositories { mavenCentral() }\ndependencies { implementation files('libs/"+jar.name+"') }")
(target/'build.gradle').write_text(build, encoding='utf-8')
metadata = target/'src/gametest/resources/fabric.mod.json'
data = json.loads(metadata.read_text())
data['entrypoints']['fabric-client-gametest'].insert(0,'io.github.r3neer.lodestonetransit.BinaryOriginCheck')
metadata.write_text(json.dumps(data,indent=2),encoding='utf-8')
(target/'src/gametest/java/io/github/r3neer/lodestonetransit/BinaryOriginCheck.java').write_text('''package io.github.r3neer.lodestonetransit;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
public final class BinaryOriginCheck implements FabricClientGameTest {
    public void runTest(ClientGameTestContext context) {
        var mod = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("lodestone_transit").orElseThrow();
        var origins = mod.getOrigin().getPaths();
        if (origins.isEmpty() || origins.stream().anyMatch(p -> !p.toString().endsWith(".jar")))
            throw new AssertionError("Release was not loaded from a JAR: " + origins);
        System.out.println("VERIFIED BINARY MOD ORIGIN: " + origins);
    }
}
''',encoding='utf-8')
print(f'Binary validation project: {target}; release {version}')
