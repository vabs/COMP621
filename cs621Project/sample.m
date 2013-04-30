a = zeros(10001,1);
d = zeros(10001,1);
x = zeros(10001,1);
tic();
for i=1:1:5000
  d(i+1) = d(i) + 20;
  x(i) = a(i+1);
end
toc();