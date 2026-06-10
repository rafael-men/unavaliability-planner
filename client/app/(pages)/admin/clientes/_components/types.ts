export interface Cliente {
  id: number;
  nome: string;
  descricao: string | null;
  ativo: boolean;
}

export interface UserClienteLink {
  user_id: number;
  cliente_id: number;
  ativo: boolean;
}
