/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.nativeplatform.fixtures.app

import org.gradle.integtests.fixtures.SourceFile

class CPCHHelloWorldApp extends IncrementalHelloWorldApp {

    @Override
    SourceFile getMainSource() {
        sourceFile("c", "main.c", """
            // Simple hello world app
            #include <stdio.h>
            #include "hello.h"

            int main () {
                sayHello();
                printf("%d", sum(5, 7));
                return 0;
            }
        """);
    }

    @Override
    SourceFile getLibraryHeader() {
        getLibraryHeader("")
    }

    SourceFile getLibraryHeader(String path) {
        sourceFile("headers/${path}", "hello.h", """
            #ifndef HELLO_H
            #define HELLO_H
            #ifdef _WIN32
            #define DLL_FUNC __declspec(dllexport)
            #else
            #define DLL_FUNC
            #endif

            void DLL_FUNC sayHello();
            int DLL_FUNC sum(int a, int b);

            #pragma message("<==== compiling hello.h ====>")
            #endif
        """);
    }

    @Override
    List<SourceFile> getLibrarySources() {
        return getLibrarySources("")
    }

    List<SourceFile> getLibrarySources(String headerPath) {
        return [
                sourceFile("c", "hello.c", """
                #include "${headerPath}hello.h"
                #include <stdio.h>

                #ifdef FRENCH
                char* greeting() {
                    return "${HELLO_WORLD_FRENCH}";
                }
                #endif
                #ifdef CUSTOM
                char* greeting() {
                    return CUSTOM;
                }
                #endif
                void DLL_FUNC sayHello() {
                    #if defined(FRENCH) || defined(CUSTOM)
                    printf("%s\\n", greeting());
                    #else
                    printf("${HELLO_WORLD}\\n");
                    #endif
                    fflush(stdout);
                }
            """),
                sourceFile("c", "sum.c", """
                #include "${headerPath}hello.h"

                int DLL_FUNC sum(int a, int b) {
                    return a + b;
                }
            """)
        ]
    }

    SourceFile getSystemHeader() {
        return getSystemHeader("")
    }

    SourceFile getSystemHeader(String path) {
        sourceFile("headers/${path}", "systemHeader.h", """
            #ifndef SYSTEMHEADER_H
            #define SYSTEMHEADER_H
            #ifdef _WIN32
            #define DLL_FUNC __declspec(dllexport)
            #else
            #define DLL_FUNC
            #endif
            void DLL_FUNC systemCall();
            #pragma message("<==== compiling systemHeader.h ====>")
            #endif
        """)
    }

    SourceFile getAlternateMainSource() {
        sourceFile("c", "main.c", """
            #include "hello.h"

            int main () {
              sayHello();
              printf("goodbye");
              return 0;
            }
        """)
    }

    String alternateOutput = "$HELLO_WORLD\ngoodbye"

    List<SourceFile> alternateLibrarySources = [
            sourceFile("c", "hello.c", """
                #include <stdio.h>
                #include "hello.h"

                void DLL_FUNC sayHello() {
                    printf("[${HELLO_WORLD} - ${HELLO_WORLD_FRENCH}]\\n");
                    fflush(stdout);
                }

                // Extra function to ensure library has different size
                int anotherFunction() {
                    return 1000;
                }
            """),
            sourceFile("c", "sum.c","""
                #include "hello.h"

                int DLL_FUNC sum(int a, int b) {
                    return a + b;
                }
            """)
    ]

    String alternateLibraryOutput = "[${HELLO_WORLD} - ${HELLO_WORLD_FRENCH}]\n12"

    TestNativeComponent getCunitTests() {
        return new TestNativeComponent() {
            List<SourceFile> sourceFiles = [
                    sourceFile("c", "test.c", """
#include <CUnit/Basic.h>
#include "hello.h"
#include "gradle_cunit_register.h"

int init_test(void) {
    return 0;
}

int clean_test(void) {
    return 0;
}

void test_sum(void) {
  CU_ASSERT(sum(0, 2) == 2);
#ifndef ONE_TEST
  CU_ASSERT(sum(0, -2) == -2);
  CU_ASSERT(sum(2, 2) == 4);
#endif
}

void gradle_cunit_register() {
    CU_pSuite pSuiteMath = CU_add_suite("hello test", init_test, clean_test);
    CU_add_test(pSuiteMath, "test_sum", test_sum);
}
                    """),
            ]
            List<SourceFile> headerFiles = [
            ]

            String testOutput = """
Suite: hello test
  Test: test of sum ...passed

Run Summary:    Type  Total    Ran Passed Failed Inactive
              suites      1      1    n/a      0        0
               tests      1      1      1      0        0
             asserts      3      3      3      0      n/a
"""
        };
    }
}
