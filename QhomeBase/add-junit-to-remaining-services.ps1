# PowerShell script to add JUnit dependencies to remaining microservices

$services = @(
    "asset-maintenance-service",
    "customer-interaction-service", 
    "data-docs-service",
    "finance-billing-service",
    "services-card-service",
    "staff-work-service"
)

$junitDependencies = @"
        
        <!-- JUnit 5 Dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-params</artifactId>
        </dependency>
        
        <!-- Mockito for Mocking -->
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
        </dependency>
        
        <!-- AssertJ for Fluent Assertions -->
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
        </dependency>
        
        <!-- TestContainers for Integration Testing -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
"@

$mavenPlugins = @"
            
            <!-- Maven Surefire Plugin for running JUnit tests -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
                <configuration>
                    <includes>
                        <include>**/*Test.java</include>
                        <include>**/*Tests.java</include>
                    </includes>
                    <excludes>
                        <exclude>**/*IntegrationTest.java</exclude>
                    </excludes>
                    <argLine>-Xmx1024m</argLine>
                </configuration>
            </plugin>
            
            <!-- Maven Failsafe Plugin for integration tests -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>3.2.5</version>
                <configuration>
                    <includes>
                        <include>**/*IntegrationTest.java</include>
                        <include>**/*IT.java</include>
                    </includes>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            
            <!-- JaCoCo Plugin for test coverage -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.11</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
"@

foreach ($service in $services) {
    $pomPath = "$service\pom.xml"
    
    if (Test-Path $pomPath) {
        Write-Host "Processing $service..."
        
        # Read the pom.xml file
        $content = Get-Content $pomPath -Raw
        
        # Add JUnit dependencies before the closing </dependencies> tag
        if ($content -match '(\s*)(</dependencies>)') {
            $content = $content -replace '(\s*)(</dependencies>)', "$junitDependencies`n$1$2"
        }
        
        # Add Maven plugins before the closing </plugins> tag in <build>
        if ($content -match '(\s*)(</plugins>)') {
            $content = $content -replace '(\s*)(</plugins>)', "$mavenPlugins`n$1$2"
        }
        
        # Write the updated content back
        Set-Content $pomPath $content -Encoding UTF8
        Write-Host "Added JUnit to $service"
    } else {
        Write-Host "$pomPath not found"
    }
}

Write-Host "JUnit dependencies added to all remaining services!"
Write-Host "Run 'mvn clean install' to download dependencies."