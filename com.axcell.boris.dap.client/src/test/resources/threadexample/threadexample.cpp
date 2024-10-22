#include <iostream>
#include <thread>
using namespace std;

void doSomethingElse() {
  float f = 42.0;
  double double_d = 30.11;
  const char* c = "constant char string";

  for (int qwerty = 0; qwerty < 10; qwerty++) {
    double some_double = 42.31;
    double another_double = 13.5243;
  }
}

void doSomething() {
  doSomethingElse();
}

void foo() {
  float f = 42.0;
  double double_d = 30.11;
  const char* c = "constant char string";

  for (int i = 0; i < 100; i ++) {
    int x = i * 3;
    int y = i + 1;
    int z = x + y;
    doSomething();
  }
}

void fubar() {
  for (int i = 0; i < 10; i ++) {
    int x = i * 30;
    int y = i + 42;
    int z = x + y;
    doSomething();
  }
}

int main() 
{
  cout << "Threading Example" << endl;

  thread first(foo);
  thread second(fubar);

  cout << "main, executing foo and fubar concurrently.." << endl;

  first.join();
  second.join();

  cout << "main, foo and fubar completed" << endl;

  return 0;
}
