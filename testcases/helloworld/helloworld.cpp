#include <iostream>
using namespace std;

void foo();

int main() 
{
  cout << "Hello, World!" << endl;

  foo();

  float f = 42.0;
  double double_d = 30.11;
  const char* c = "constant char string";

  for (int i = 0; i < 3; i++) {
    cout << "loop: " << i << endl;
  }

  cout << "Goodbye, Cruel World!" << endl;
  return 0;
}

void foo() {
  int numPlanets = 8;
  cout << "function foo" << endl;
}
