import { Component, ElementRef, OnDestroy, OnInit, ViewChild, input, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';

export interface MeshData {
  nodes: number[][];
  elements: number[][];
  displacements?: number[][];
  stresses?: number[];
}

@Component({
  selector: 'app-mesh-viewer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="viewer-container">
      <div class="viewer-toolbar">
        <button (click)="resetCamera()">Reset View</button>
        <button (click)="toggleWireframe()">Wireframe</button>
        <button (click)="toggleDisplacements()">Deformation</button>
        <select (change)="setColorMap($event)">
          <option value="displacement">Displacement</option>
          <option value="stress">Von Mises Stress</option>
        </select>
        <label>
          Scale: <input type="range" min="1" max="100" value="10" (input)="setDeformationScale($event)">
        </label>
      </div>
      <canvas #canvas></canvas>
      <div class="viewer-info">
        <span>Nodes: {{ meshData()?.nodes?.length || 0 }}</span>
        <span>Elements: {{ meshData()?.elements?.length || 0 }}</span>
      </div>
    </div>
  `,
  styles: [`
    .viewer-container {
      position: relative;
      width: 100%;
      height: 500px;
      background: #1a1a2e;
      border-radius: 8px;
      overflow: hidden;
    }
    canvas { width: 100%; height: 100%; display: block; }
    .viewer-toolbar {
      position: absolute;
      top: 10px;
      left: 10px;
      z-index: 10;
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .viewer-toolbar button, .viewer-toolbar select {
      padding: 4px 10px;
      background: rgba(255,255,255,0.1);
      border: 1px solid rgba(255,255,255,0.3);
      color: white;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;
    }
    .viewer-toolbar label { color: white; font-size: 12px; }
    .viewer-info {
      position: absolute;
      bottom: 10px;
      right: 10px;
      color: rgba(255,255,255,0.7);
      font-size: 11px;
      display: flex;
      gap: 12px;
    }
  `]
})
export class MeshViewerComponent implements OnInit, OnDestroy {
  @ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

  meshData = input<MeshData | null>(null);

  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private renderer!: THREE.WebGLRenderer;
  private controls!: OrbitControls;
  private meshGroup = new THREE.Group();
  private wireframeVisible = false;
  private showDisplacements = true;
  private deformationScale = 10;
  private animationId = 0;

  constructor() {
    effect(() => {
      const data = this.meshData();
      if (data) this.buildMesh(data);
    });
  }

  ngOnInit(): void {
    this.initScene();
    this.animate();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animationId);
    this.renderer.dispose();
  }

  private initScene(): void {
    const canvas = this.canvasRef.nativeElement;
    this.scene = new THREE.Scene();
    this.scene.background = new THREE.Color(0x1a1a2e);

    this.camera = new THREE.PerspectiveCamera(60, canvas.clientWidth / canvas.clientHeight, 0.1, 1000);
    this.camera.position.set(5, 5, 5);

    this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
    this.renderer.setSize(canvas.clientWidth, canvas.clientHeight);
    this.renderer.setPixelRatio(window.devicePixelRatio);

    this.controls = new OrbitControls(this.camera, canvas);
    this.controls.enableDamping = true;

    this.scene.add(new THREE.AmbientLight(0xffffff, 0.4));
    const dirLight = new THREE.DirectionalLight(0xffffff, 0.8);
    dirLight.position.set(5, 10, 5);
    this.scene.add(dirLight);

    this.scene.add(new THREE.GridHelper(10, 10, 0x444444, 0x222222));
    this.scene.add(this.meshGroup);
  }

  private animate(): void {
    this.animationId = requestAnimationFrame(() => this.animate());
    this.controls.update();
    this.renderer.render(this.scene, this.camera);
  }

  private buildMesh(data: MeshData): void {
    this.meshGroup.clear();

    const geometry = new THREE.BufferGeometry();
    const vertices: number[] = [];
    const colors: number[] = [];

    const maxDisp = data.displacements
      ? Math.max(...data.displacements.map(d => Math.sqrt(d[0]**2 + d[1]**2 + d[2]**2)))
      : 1;

    for (const element of data.elements) {
      const faces = this.tetrahedronFaces(element);
      for (const face of faces) {
        for (const nodeIdx of face) {
          const node = data.nodes[nodeIdx];
          let x = node[0], y = node[1], z = node[2];

          if (this.showDisplacements && data.displacements) {
            const d = data.displacements[nodeIdx];
            x += d[0] * this.deformationScale;
            y += d[1] * this.deformationScale;
            z += d[2] * this.deformationScale;
          }

          vertices.push(x, y, z);

          if (data.displacements) {
            const d = data.displacements[nodeIdx];
            const mag = Math.sqrt(d[0]**2 + d[1]**2 + d[2]**2) / maxDisp;
            const color = new THREE.Color().setHSL(0.66 - mag * 0.66, 1, 0.5);
            colors.push(color.r, color.g, color.b);
          } else {
            colors.push(0.3, 0.5, 0.8);
          }
        }
      }
    }

    geometry.setAttribute('position', new THREE.Float32BufferAttribute(vertices, 3));
    geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));
    geometry.computeVertexNormals();

    const material = new THREE.MeshPhongMaterial({
      vertexColors: true,
      side: THREE.DoubleSide,
      flatShading: true
    });

    const mesh = new THREE.Mesh(geometry, material);
    this.meshGroup.add(mesh);

    if (this.wireframeVisible) {
      const wireframe = new THREE.WireframeGeometry(geometry);
      const line = new THREE.LineSegments(wireframe, new THREE.LineBasicMaterial({ color: 0xffffff, opacity: 0.3, transparent: true }));
      this.meshGroup.add(line);
    }
  }

  private tetrahedronFaces(element: number[]): number[][] {
    return [
      [element[0], element[1], element[2]],
      [element[0], element[1], element[3]],
      [element[0], element[2], element[3]],
      [element[1], element[2], element[3]]
    ];
  }

  resetCamera(): void {
    this.camera.position.set(5, 5, 5);
    this.controls.reset();
  }

  toggleWireframe(): void {
    this.wireframeVisible = !this.wireframeVisible;
    const data = this.meshData();
    if (data) this.buildMesh(data);
  }

  toggleDisplacements(): void {
    this.showDisplacements = !this.showDisplacements;
    const data = this.meshData();
    if (data) this.buildMesh(data);
  }

  setDeformationScale(event: Event): void {
    this.deformationScale = +(event.target as HTMLInputElement).value;
    const data = this.meshData();
    if (data) this.buildMesh(data);
  }

  setColorMap(event: Event): void {
    const data = this.meshData();
    if (data) this.buildMesh(data);
  }
}
